package com.behsazan.corebanking.iam.s1b;

import com.behsazan.corebanking.iam.s1b.IamS1bModels.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LOCAL LAB CONTRACT, not bank IdP acceptance. This spins up a real HTTP server and
 * the PRODUCTION IAM security filter chain, but substitutes a test-only RS256 public-key
 * decoder and mock repository. No real Oracle, credentials, token files or DML.
 */
@SpringBootTest(classes=IamS1bSignedHttpIntegrationTest.LabApp.class,
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"core-banking.iam.s1b.reads-enabled=true", "server.address=127.0.0.1", "spring.main.banner-mode=off"})
class IamS1bSignedHttpIntegrationTest {
    private static final String ISSUER="https://iam-test.invalid/issuer";
    private static final String AUDIENCE="core-iam-read-lab";
    private static final String DOMAIN="DEMO_WF";
    private static final String CONTEXT="WORKFORCE";
    private static final String PATH="/api/v1/iam/readiness?domainCode="+DOMAIN;
    private static final int MAX_SECONDS=1800;

    @SpringBootConfiguration(proxyBeanMethods=false)
    @EnableAutoConfiguration(exclude=DataSourceAutoConfiguration.class)
    @Import({IamS1bSecurityConfiguration.class, IamS1bReadController.class,
             IamS1bReadService.class, IamS1bExceptionHandler.class})
    static class LabApp {
        @Bean IamS1bProperties iamS1bProperties() {
            return new IamS1bProperties(true, ISSUER, "https://iam-test.invalid/jwks", AUDIENCE,
                    MAX_SECONDS, "jdbc:oracle:thin:@//localhost:1521/never_connect",
                    "IAM_TEST_READ_ONLY", "test-only-never-used");
        }
        @Bean IamS1bReadRepository iamS1bReadRepository() {
            return mock(IamS1bReadRepository.class);
        }
    }

    // The production JWKS decoder is conditional and need not be registered in this isolated LabApp.
    // REPLACE_OR_CREATE is test-only: bind the named IAM decoder without weakening production security.
    // Every token remains cryptographically verified by a real Nimbus RS256 decoder in setup().
    @MockitoBean(name="iamS1bJwtDecoder")
    JwtDecoder labDecoder;
    @Autowired IamS1bReadRepository repository;
    @Autowired Environment environment;
    private KeyPair key;
    private HttpClient client;

    @BeforeEach void setup() throws Exception {
        key=IamS1bRs256Fixture.freshKeyPair();
        client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        NimbusJwtDecoder realVerifier=NimbusJwtDecoder.withPublicKey((RSAPublicKey) key.getPublic()).build();
        realVerifier.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                IamS1bTokenValidation.requiredClaims(AUDIENCE, MAX_SECONDS)));
        reset(labDecoder, repository);
        when(labDecoder.decode(anyString())).thenAnswer(call -> realVerifier.decode(call.getArgument(0)));
        when(repository.metadataReady()).thenReturn(true);
        when(repository.domain(DOMAIN)).thenReturn(Optional.of(new Domain(7L,DOMAIN,CONTEXT,true)));
        when(repository.accounts(any(), nullable(String.class), anyInt(), anyInt())).thenReturn(List.of());
    }

    private String token(String issuer, String audience, String scope, String domain,
                         String context, long issued, long expires) throws Exception {
        return IamS1bRs256Fixture.token(key.getPrivate(), issuer, audience, scope,
                domain, context, issued, expires);
    }
    private String token(String scope, String domain) throws Exception {
        long now=Instant.now().getEpochSecond();
        return token(ISSUER,AUDIENCE,scope,domain,CONTEXT,now-10,now+600);
    }
    private HttpResponse<String> request(String path, String method, String jwt) throws Exception {
        int port=Integer.parseInt(environment.getRequiredProperty("local.server.port"));
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
                .timeout(Duration.ofSeconds(8)).header("Accept","application/json");
        if(jwt!=null)builder.header("Authorization","Bearer "+jwt);
        builder.method(method, HttpRequest.BodyPublishers.noBody());
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    private void expect(String name, int status, String path, String method, String jwt) throws Exception {
        assertThat(request(path,method,jwt).statusCode()).as(name).isEqualTo(status);
    }

    @Test void realSignedHttp401403200503Matrix() throws Exception {
        long now=Instant.now().getEpochSecond();
        String good=token("iam.users.read",DOMAIN);
        // 401 requires a missing/invalid access token, not a forged user header.
        expect("no bearer",401,PATH,"GET",null);
        expect("broken JWT",401,PATH,"GET","invalid.jwt.token");
        String wrongIssuer=token("https://different-issuer.invalid",AUDIENCE,"iam.users.read",DOMAIN,CONTEXT,now-10,now+600);
        expect("wrong issuer",401,PATH,"GET",wrongIssuer);
        String wrongAudience=token(ISSUER,"different-api","iam.users.read",DOMAIN,CONTEXT,now-10,now+600);
        expect("wrong audience",401,PATH,"GET",wrongAudience);
        String expired=token(ISSUER,AUDIENCE,"iam.users.read",DOMAIN,CONTEXT,now-3600,now-300);
        expect("expired token",401,PATH,"GET",expired);
        String[] jwtParts=good.split("\\.");
        int middle=jwtParts[2].length()/2;
        String sig=jwtParts[2];
        String tampered=jwtParts[0]+"."+jwtParts[1]+"."+sig.substring(0,middle)
                +(sig.charAt(middle)=='A'?'B':'A')+sig.substring(middle+1);
        expect("tampered signature",401,PATH,"GET",tampered);
        // A valid signed token without a proper scope or exact domain is 403 and MUST avoid Oracle I/O.
        clearInvocations(repository);
        expect("scope denied",403,PATH,"GET",token("some.other.scope",DOMAIN));
        expect("domain denied before database",403,PATH,"GET",token("iam.users.read","OTHER_WF"));
        verifyNoInteractions(repository);
        expect("POST forbidden",403,"/api/v1/iam/accounts?domainCode="+DOMAIN,"POST",good);
        // The 200 response uses a fake in-memory repository. NEVER claim a real Oracle read.
        HttpResponse<String> readiness=request(PATH,"GET",good);
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("R1.5.1-AS-IS").contains("DEMO_WF");
        expect("list over HTTPS bearer",200,"/api/v1/iam/accounts?domainCode="+DOMAIN,"GET",good);
        // Real service error mapping through HTTP, but DB unavailable is mocked for this laboratory case.
        when(repository.metadataReady()).thenReturn(false);
        expect("database unavailable",503,PATH,"GET",good);
        System.out.println("IAM_S1B_RO3_SIGNED_HTTP_LAB_PASS=12/12; RS256_VERIFIED; MOCK_REPO; NO_ORACLE_IO");
    }
}
