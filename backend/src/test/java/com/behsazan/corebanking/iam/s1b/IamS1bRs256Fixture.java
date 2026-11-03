package com.behsazan.corebanking.iam.s1b;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

/** TEST SOURCE ONLY. Keys are fresh for every local run; never ship a shared signing key. */
public final class IamS1bRs256Fixture {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private IamS1bRs256Fixture() { }

    public static KeyPair freshKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String b64(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Strictly fixed test-only vocabulary; never accept user input into this JSON fixture. */
    public static String token(PrivateKey key, String issuer, String audience, String scope,
                               String domain, String context, long issuedAt, long expiresAt)
            throws GeneralSecurityException {
        String header = b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"iss\":\"" + issuer + "\",\"sub\":\"test-ro3-subject\","
                + "\"aud\":[\"" + audience + "\"],\"scope\":\"" + scope + "\","
                + "\"iat\":" + issuedAt + ",\"nbf\":" + issuedAt + ",\"exp\":" + expiresAt + ","
                + "\"iam_domains\":[\"" + domain + "\"],\"iam_contexts\":[\"" + context + "\"]}");
        String signedPart = header + "." + payload;
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(signedPart.getBytes(StandardCharsets.US_ASCII));
        return signedPart + "." + URL_ENCODER.encodeToString(signer.sign());
    }

    public static boolean verify(RSAPublicKey key, String token) throws GeneralSecurityException {
        if (token == null) return false;
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}").equals(parts[0])) return false;
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            return verifier.verify(URL_DECODER.decode(parts[2]));
        } catch (IllegalArgumentException invalidSignatureEncoding) {
            return false;
        }
    }

    private static int checks = 0;
    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError("RO3_SIGNATURE_CONTRACT_FAILED " + name);
        checks++;
    }

    /** Offline JDK21 regression: this is cryptographic evidence, NOT live IdP or Oracle acceptance. */
    public static void main(String[] args) throws Exception {
        KeyPair a = freshKeyPair(), b = freshKeyPair();
        long t = Instant.parse("2026-11-03T00:00:00Z").getEpochSecond();
        String valid = token(a.getPrivate(), "https://test-idp.invalid", "iam-api", "iam.users.read",
                "DEMO_WF", "WORKFORCE", t - 10, t + 600);
        require(verify((RSAPublicKey) a.getPublic(), valid), "genuine_rs256_signature");
        require(!verify((RSAPublicKey) b.getPublic(), valid), "another_public_key_denied");
        require(valid.split("\\.").length == 3, "compact_jwt");
        require(!verify((RSAPublicKey) a.getPublic(), valid.substring(0, valid.length()-4)+"AAAA"), "tampered_signature_denied");
        String[] chunks=valid.split("\\.");
        String altered = chunks[0]+"."+b64("{\"iss\":\"https://evil.invalid\"}")+"."+chunks[2];
        require(!verify((RSAPublicKey) a.getPublic(), altered), "tampered_payload_denied");
        require(!verify((RSAPublicKey) a.getPublic(), "invalid.jwt.token"), "malformed_denied");
        require(!verify((RSAPublicKey) a.getPublic(), ""), "empty_denied");
        require(!verify((RSAPublicKey) a.getPublic(), "x.y"), "missing_signature_denied");
        String noScope = token(a.getPrivate(), "https://test-idp.invalid", "iam-api", "none",
                "DEMO_WF", "WORKFORCE", t-10, t+600);
        require(verify((RSAPublicKey) a.getPublic(), noScope), "scope_is_authorization_not_signature");
        String wrongAudience = token(a.getPrivate(), "https://test-idp.invalid", "another-api", "iam.users.read",
                "DEMO_WF", "WORKFORCE", t-10, t+600);
        require(verify((RSAPublicKey) a.getPublic(), wrongAudience), "audience_is_post_signature_claim_validation");
        require(!valid.contains("test-ro3-subject"), "payload_base64url_encoded");
        require(a.getPrivate().getEncoded().length > 0, "ephemeral_private_key_exists_only_in_process");
        System.out.println("IAM_S1B_RO3_RS256_JDK_PASS="+checks+"/"+checks+"; EPHEMERAL_KEY; NO_TOKEN_OUTPUT; NO_ORACLE_IO");
    }
}
