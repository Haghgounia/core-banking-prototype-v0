package com.behsazan.corebanking.collateral.reference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** One-click CLT local prototype import; re-runs only retry rows with a NULL LOGO. */
@Service
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltInsuranceLogoImportService {
    private static final String TABLE = "CLT.INSURANCE_COMPANY";
    private static final String ACTOR = "CLT_LOGO_IMPORT_LOCAL";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final HttpClient client;
    private final List<LogoSource> sources;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public record Company(long id, String name, long version, long logoBytes, String websiteUrl) { }
    public record LogoSource(String companyName, String imageUrl) { }
    public record CompanyView(String id, String name, String websiteUrl, long logoBytes, boolean sampleAvailable) { }
    public record Status(boolean ready, boolean processing, int companyCount, int websiteCount,
                         int logoCount, int sampleSources, int pendingSamples,
                         List<CompanyView> companies, String issue) { }
    public record Item(String companyName, String outcome, String detail) { }
    public record ImportResult(int totalSources, int imported, int alreadyPresent,
                               int failed, int unmatched, List<Item> results, Status status) { }
    private record Prepared(LogoSource source, Company company, byte[] bytes, String failure) { }

    public CltInsuranceLogoImportService(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(12);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setTimeout(12);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        sources = readSources();
    }

    private static List<LogoSource> readSources() {
        List<LogoSource> result = new ArrayList<>();
        try (InputStream stream = new ClassPathResource("clt/insurance-logo-sources.tsv").getInputStream()) {
            String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : body.split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length != 2 || parts[0].isBlank()) throw new IllegalStateException("CLT_LOGO_SOURCE_INVALID");
                CltInsuranceImagePolicy.sourceUri(parts[1]);
                result.add(new LogoSource(parts[0].trim(), parts[1]));
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("CLT_LOGO_SOURCE_MANIFEST_UNAVAILABLE", e);
        }
        if (result.size() != 18 || result.stream().map(LogoSource::companyName).distinct().count() != 18)
            throw new IllegalStateException("CLT_LOGO_SOURCE_COUNT_INVALID");
        return List.copyOf(result);
    }

    private static String matchName(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFKC)
                .replace('ي', 'ی').replace('ك', 'ک').replaceAll("\\s+", " ").trim();
    }

    private List<Company> companies() {
        return jdbc.query("SELECT INSURANCE_COMPANY_ID, INSURANCE_COMPANY_NAME, RECORD_VERSION, "
                + "NVL(DBMS_LOB.GETLENGTH(LOGO),0) AS LOGO_BYTES, WEBSITE_URL FROM " + TABLE
                + " ORDER BY INSURANCE_COMPANY_ID", (rs, n) -> new Company(
                rs.getLong("INSURANCE_COMPANY_ID"), rs.getString("INSURANCE_COMPANY_NAME"),
                rs.getLong("RECORD_VERSION"), rs.getLong("LOGO_BYTES"), rs.getString("WEBSITE_URL")));
    }

    public Status status() {
        try {
            List<Company> companyList = companies();
            Map<String, Integer> counted = new HashMap<>();
            for (LogoSource source : sources) counted.merge(matchName(source.companyName()), 1, Integer::sum);
            List<CompanyView> view = companyList.stream().map(company ->
                    new CompanyView(Long.toString(company.id()), company.name(), company.websiteUrl(),
                            company.logoBytes(), counted.containsKey(matchName(company.name()))))
                    .toList();
            int pending = (int) view.stream().filter(c -> c.sampleAvailable() && c.logoBytes() == 0).count();
            return new Status(true, running.get(), view.size(),
                    (int) view.stream().filter(c -> c.websiteUrl() != null && !c.websiteUrl().isBlank()).count(),
                    (int) view.stream().filter(c -> c.logoBytes() > 0).count(),
                    sources.size(), pending, view, "");
        } catch (org.springframework.dao.DataAccessException e) {
            return new Status(false, running.get(), 0, 0, 0, sources.size(), 0, List.of(),
                    "ساختار یا دسترسی خواندن اطلاعات لوگو در پایگاه داده آماده نیست.");
        }
    }

    public ImportResult importOnce() {
        if (!running.compareAndSet(false, true))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_LOGO_IMPORT_ALREADY_RUNNING");
        try {
            Status before = status();
            if (!before.ready()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_LOGO_COLUMNS_OR_READS_NOT_READY");
            if (before.companyCount() != 42)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_LOGO_COMPANY_BASELINE_COUNT_CHANGED");
            List<Company> existing = companies();
            Map<String, List<Company>> byName = new HashMap<>();
            existing.forEach(c -> byName.computeIfAbsent(matchName(c.name()), k -> new ArrayList<>()).add(c));
            List<Item> results = new ArrayList<>();
            List<Callable<Prepared>> tasks = new ArrayList<>();
            for (LogoSource source : sources) {
                List<Company> matches = byName.getOrDefault(matchName(source.companyName()), List.of());
                if (matches.size() != 1) {
                    results.add(new Item(source.companyName(), "NAME_MISMATCH", "تطبیق یکتای شرکت برقرار نیست."));
                    continue;
                }
                Company company = matches.getFirst();
                if (company.logoBytes() > 0) {
                    results.add(new Item(source.companyName(), "ALREADY_PRESENT", "لوگوی ذخیره‌شده حفظ شد."));
                    continue;
                }
                tasks.add(() -> fetch(source, company));
            }
            // Bounded parallel network I/O. The database is touched only after each file validates.
            ExecutorService pool = Executors.newFixedThreadPool(4);
            try {
                List<Future<Prepared>> fetched = pool.invokeAll(tasks, 50, TimeUnit.SECONDS);
                for (int n = 0; n < fetched.size(); n++) {
                    Prepared p;
                    Future<Prepared> future = fetched.get(n);
                    if (future.isCancelled()) {
                        results.add(new Item("دریافت شماره " + (n + 1), "DOWNLOAD_FAILED", "مهلت دریافت تصویر پایان یافت."));
                        continue;
                    }
                    try { p = future.get(); }
                    catch (Exception e) {
                        results.add(new Item("دریافت شماره " + (n + 1), "DOWNLOAD_FAILED", "خطای دریافت یا پردازش تصویر."));
                        continue;
                    }
                    if (p.failure() != null) {
                        results.add(new Item(p.source().companyName(), "DOWNLOAD_FAILED", p.failure()));
                        continue;
                    }
                    try {
                        boolean saved = transaction.execute(s -> persist(p));
                        results.add(new Item(p.source().companyName(), saved ? "IMPORTED" : "WRITE_CONFLICT",
                                saved ? "تصویر در پایگاه داده ذخیره و بازخوانی شد." : "رکورد در این فاصله تغییر کرده است."));
                    } catch (RuntimeException e) {
                        results.add(new Item(p.source().companyName(), "WRITE_FAILED", "ثبت یا تأیید تصویر انجام نشد."));
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_LOGO_IMPORT_INTERRUPTED");
            } finally { pool.shutdownNow(); }
            int imported = (int) results.stream().filter(x -> "IMPORTED".equals(x.outcome())).count();
            int present = (int) results.stream().filter(x -> "ALREADY_PRESENT".equals(x.outcome())).count();
            int unmatched = (int) results.stream().filter(x -> "NAME_MISMATCH".equals(x.outcome())).count();
            int failed = results.size() - imported - present - unmatched;
            return new ImportResult(sources.size(), imported, present, failed, unmatched,
                    List.copyOf(results), status());
        } finally { running.set(false); }
    }

    private Prepared fetch(LogoSource source, Company company) {
        try {
            URI original = CltInsuranceImagePolicy.sourceUri(source.imageUrl());
            URI current = original;
            for (int redirects = 0; redirects <= 2; redirects++) {
                for (InetAddress address : InetAddress.getAllByName(current.getHost())) {
                    if (!CltInsuranceImagePolicy.publicAddress(address))
                        throw new IllegalArgumentException("IMAGE_ADDRESS_NOT_PUBLIC");
                }
                HttpRequest req = HttpRequest.newBuilder(current).GET()
                        .timeout(Duration.ofSeconds(9))
                        .header("Accept", "image/png,image/jpeg,image/gif")
                        .header("User-Agent", "CLT-Logo-Importer/1.0")
                        .build();
                HttpResponse<InputStream> response = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream input = response.body()) {
                    if (response.statusCode() >= 300 && response.statusCode() < 400) {
                        String location = response.headers().firstValue("Location").orElse("");
                        if (location.isBlank() || redirects == 2)
                            throw new IllegalArgumentException("IMAGE_REDIRECT_NOT_ALLOWED");
                        current = CltInsuranceImagePolicy.safeRedirect(original, current, location);
                        continue;
                    }
                    if (response.statusCode() != 200)
                        return new Prepared(source, company, null, "منبع تصویر پاسخ موفق نداد (HTTP " + response.statusCode() + ").");
                    byte[] bytes = input.readNBytes(CltInsuranceImagePolicy.MAX_BYTES + 1);
                    if (bytes.length > CltInsuranceImagePolicy.MAX_BYTES)
                        throw new IllegalArgumentException("IMAGE_TOO_LARGE");
                    CltInsuranceImagePolicy.checkedContentType(bytes);
                    return new Prepared(source, company, bytes, null);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Prepared(source, company, null, "دریافت تصویر متوقف شد.");
        } catch (IOException e) {
            return new Prepared(source, company, null, "لینک تصویر در دسترس نیست یا اتصال امن برقرار نشد.");
        } catch (IllegalArgumentException e) {
            return new Prepared(source, company, null, "منبع تصویر معتبر و قابل استفاده نیست: " + e.getMessage());
        } catch (RuntimeException e) {
            return new Prepared(source, company, null, "دریافت تصویر از منبع امکان‌پذیر نیست.");
        }
        return new Prepared(source, company, null, "لینک تصویر به مقصد مجاز نرسید.");
    }

    private boolean persist(Prepared prepared) {
        Company c = prepared.company();
        byte[] content = prepared.bytes();
        int affected = jdbc.update(connection -> {
            var ps = connection.prepareStatement("UPDATE " + TABLE + " SET LOGO=?, "
                    + "UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=?, RECORD_VERSION=RECORD_VERSION+1 "
                    + "WHERE INSURANCE_COMPANY_ID=? AND RECORD_VERSION=? AND LOGO IS NULL");
            ps.setBinaryStream(1, new ByteArrayInputStream(content), content.length);
            ps.setString(2, ACTOR);
            ps.setLong(3, c.id());
            ps.setLong(4, c.version());
            return ps;
        });
        if (affected != 1) return false;
        byte[] readBack = image(c.id());
        if (!MessageDigest.isEqual(sha256(content), sha256(readBack)))
            throw new IllegalStateException("CLT_LOGO_BLOB_READBACK_MISMATCH");
        return true;
    }

    private static byte[] sha256(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA256_UNAVAILABLE", e); }
    }

    public byte[] image(long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_INVALID_ID");
        List<byte[]> images = jdbc.query("SELECT LOGO FROM " + TABLE + " WHERE INSURANCE_COMPANY_ID=?",
                (rs, rowNum) -> rs.getBytes(1), id);
        if (images.isEmpty() || images.getFirst() == null || images.getFirst().length == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CLT_LOGO_NOT_FOUND");
        if (images.getFirst().length > CltInsuranceImagePolicy.MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLT_LOGO_TOO_LARGE");
        return images.getFirst();
    }
}
