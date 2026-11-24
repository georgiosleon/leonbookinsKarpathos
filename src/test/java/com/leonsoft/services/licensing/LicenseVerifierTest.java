package com.leonsoft.services.licensing;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class LicenseVerifierTest {

    private static final String PRIVATE_KEY_B64 = "MC4CAQAwBQYDK2VwBCIEIBIYeDSqPYckDWsZActWNugz9bKG74btoLC88v/Pu8vD";
    private static final String INSTALL_ID = "install-123";

    private final LicenseVerifier verifier = new LicenseVerifier(new ObjectMapper());

    @Test
    void verifiesValidSignedLicense() throws Exception {
        String key = sign(payload(INSTALL_ID, "Acme", "+30", "a@b.com"));
        LicensePayload payload = verifier.verify(key, INSTALL_ID);
        assertEquals("Acme", payload.customerName());
        assertEquals(INSTALL_ID, payload.installId());
        assertEquals("leonbookinsKarpathos", payload.product());
    }

    @Test
    void rejectsWrongInstallId() throws Exception {
        String key = sign(payload("other-install", "Acme", "+30", "a@b.com"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> verifier.verify(key, INSTALL_ID));
        assertTrue(ex.getReason().contains("does not match"));
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String key = sign(payload(INSTALL_ID, "Acme", "+30", "a@b.com"));
        String[] parts = key.split("\\.", 2);
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload(INSTALL_ID, "Evil", "+30", "a@b.com").getBytes(StandardCharsets.UTF_8));
        String tampered = tamperedPayload + "." + parts[1];
        assertThrows(ResponseStatusException.class, () -> verifier.verify(tampered, INSTALL_ID));
    }

    @Test
    void rejectsInvalidFormat() {
        assertThrows(ResponseStatusException.class, () -> verifier.verify("not-a-key", INSTALL_ID));
    }

    private static String payload(String installId, String name, String phone, String email) {
        return "{"
                + "\"product\":\"leonbookinsKarpathos\","
                + "\"licenseId\":\"lic-1\","
                + "\"installId\":\"" + installId + "\","
                + "\"customerName\":\"" + name + "\","
                + "\"customerTelephone\":\"" + phone + "\","
                + "\"customerEmail\":\"" + email + "\","
                + "\"issuedAt\":\"2026-11-24\""
                + "}";
    }

    private static String sign(String payloadJson) throws Exception {
        byte[] pkcs8 = Base64.getDecoder().decode(PRIVATE_KEY_B64);
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(payloadBytes);
        byte[] sig = signature.sign();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    }
}
