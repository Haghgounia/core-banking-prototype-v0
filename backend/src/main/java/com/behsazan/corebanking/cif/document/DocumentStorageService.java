package com.behsazan.corebanking.cif.document;

import com.behsazan.corebanking.cif.error.CifNotFoundException;
import com.behsazan.corebanking.cif.error.CifValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.DigestInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {
    private static final String REF_PREFIX = "cif-doc:";
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/tiff"
    );

    private final Path root;
    private final long maxFileSizeBytes;

    public DocumentStorageService(
            @Value("${core-banking.document-storage.root:./data/document-storage}") String rootDirectory,
            @Value("${core-banking.document-storage.max-file-size-bytes:20971520}") long maxFileSizeBytes
    ) {
        this.root = Path.of(rootDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public DocumentUploadResponse store(long partyId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw validation("فایل مدرک انتخاب نشده است.");
        if (file.getSize() > maxFileSizeBytes) throw validation("حجم فایل مدرک بیش از حد مجاز است.");

        String mimeType = normalizeMimeType(file.getContentType(), file.getOriginalFilename());
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw validation("نوع فایل مجاز نیست. فرمت‌های مجاز: PDF، JPEG، PNG و TIFF.");
        }

        LocalDate today = LocalDate.now();
        String extension = extensionFor(mimeType);
        String relative = "party/" + partyId + "/" + today.getYear() + "/" + String.format(Locale.ROOT, "%02d", today.getMonthValue())
                + "/" + UUID.randomUUID() + extension;
        Path target = resolveSafe(relative);
        Path temp = target.resolveSibling(target.getFileName() + ".part");

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream raw = file.getInputStream(); DigestInputStream input = new DigestInputStream(raw, digest)) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new DocumentUploadResponse(
                    REF_PREFIX + relative,
                    HexFormat.of().formatHex(digest.digest()),
                    mimeType,
                    safeOriginalName(file.getOriginalFilename()),
                    file.getSize()
            );
        } catch (NoSuchAlgorithmException | IOException exception) {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            throw new IllegalStateException("ذخیره امن فایل مدرک انجام نشد.", exception);
        }
    }

    public StoredDocument load(long partyId, String storageRef, String mimeType) {
        if (storageRef == null || !storageRef.startsWith(REF_PREFIX)) {
            throw validation("مرجع ذخیره فایل معتبر نیست.");
        }
        String relative = storageRef.substring(REF_PREFIX.length());
        if (!relative.startsWith("party/" + partyId + "/")) {
            throw validation("مرجع فایل با Party جاری تطابق ندارد.");
        }
        Path path = resolveSafe(relative);
        if (!Files.isRegularFile(path)) throw new CifNotFoundException("فایل مدرک در مخزن امن یافت نشد.");
        try {
            return new StoredDocument(new UrlResource(path.toUri()), normalizeMimeType(mimeType, path.getFileName().toString()));
        } catch (IOException exception) {
            throw new IllegalStateException("فراخوانی فایل مدرک انجام نشد.", exception);
        }
    }

    private Path resolveSafe(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) throw validation("مرجع فایل معتبر نیست.");
        return path;
    }

    private static CifValidationException validation(String message) {
        return new CifValidationException(message, Map.of("file", message));
    }

    private static String normalizeMimeType(String mimeType, String filename) {
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT).trim();
        if (!normalized.isBlank() && !"application/octet-stream".equals(normalized)) return normalized;
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) return "image/tiff";
        return "application/octet-stream";
    }

    private static String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/tiff" -> ".tiff";
            default -> "";
        };
    }

    private static String safeOriginalName(String value) {
        if (value == null || value.isBlank()) return "document";
        String normalized = value.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = (slash >= 0 ? normalized.substring(slash + 1) : normalized)
                .replaceAll("[\r\n]", "")
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (name.isBlank()) return "document";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    public record DocumentUploadResponse(
            String storageRef,
            String contentHash,
            String mimeType,
            String originalFileName,
            long size
    ) { }

    public record StoredDocument(Resource resource, String mimeType) { }
}
