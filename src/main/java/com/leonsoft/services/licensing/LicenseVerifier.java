package com.leonsoft.services.licensing;

import tools.jackson.databind.ObjectMapper;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public final class LicenseVerifier {

    static final String PRODUCT = "leonbookinsKarpathos";
    // Dev/public verification key (Ed25519, X.509). Replace for production deployments.
    static final String PUBLIC_KEY_B64 = "MCowBQYDK2VwAyEAURg0//L5zeM9lZxbFnMuCM6Emvx2BPjzXKKbnO2x31k=";

    private final ObjectMapper objectMapper;
    private final PublicKey publicKey;

    @Autowired
    public LicenseVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.publicKey = loadPublicKey(PUBLIC_KEY_B64);
    }

    LicenseVerifier(ObjectMapper objectMapper, String publicKeyB64) {
        this.objectMapper = objectMapper;
        this.publicKey = loadPublicKey(publicKeyB64);
    }

    public LicensePayload verify(String signedLicense, String expectedInstallId) {
        if (signedLicense == null || signedLicense.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key is required.");
        }
        String[] parts = signedLicense.trim().split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key format is invalid.");
        }
        byte[] payloadBytes;
        byte[] signatureBytes;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            signatureBytes = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key encoding is invalid.");
        }

        boolean valid;
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payloadBytes);
            valid = signature.verify(signatureBytes);
        } catch (java.security.GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key could not be verified.", e);
        }
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key signature is invalid.");
        }

        LicensePayload payload;
        try {
            payload = objectMapper.readValue(payloadBytes, LicensePayload.class);
        } catch (tools.jackson.core.JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key payload is invalid.", e);
        }

        if (!PRODUCT.equals(payload.product())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key is not valid for this product.");
        }
        if (payload.installId() == null || payload.installId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key install ID is missing.");
        }
        if (expectedInstallId == null || !expectedInstallId.equals(payload.installId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key does not match this installation.");
        }
        if (payload.customerName() == null || payload.customerName().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License key customer name is missing.");
        }
        return payload;
    }

    public String encodePayload(LicensePayload payload) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Could not encode license payload", e);
        }
    }

    private static PublicKey loadPublicKey(String publicKeyB64) {
        try {
            byte[] encoded = Base64.getDecoder().decode(publicKeyB64);
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load license public key", e);
        }
    }
}
