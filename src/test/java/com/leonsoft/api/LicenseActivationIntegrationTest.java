package com.leonsoft.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class LicenseActivationIntegrationTest {

    @TempDir
    static Path temporaryDirectory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Path database = temporaryDirectory.resolve("license-it.db");
        String url = "jdbc:sqlite:" + database.toAbsolutePath();
        registry.add("spring.datasource.url", () -> url);
        registry.add("APP_DB_URL", () -> url);
        registry.add("url", () -> url);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void activationUnblocksApisWithoutRestart() throws Exception {
        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("License activation required. Please contact an administrator."));

        MvcResult statusResult = mockMvc.perform(get("/api/v1/license/status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activated").value(false))
                .andExpect(jsonPath("$.installId").isNotEmpty())
                .andReturn();
        String installId = objectMapper.readTree(statusResult.getResponse().getContentAsString()).get("installId").asText();

        String licenseKey = signLicense(installId);
        mockMvc.perform(post("/api/v1/license/activate").with(csrf()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("licenseKey", licenseKey))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activated").value(true));

        mockMvc.perform(get("/api/v1/booking/getAll").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/booking/getAll").with(user("guest").roles("GUEST")))
                .andExpect(status().isOk());
    }

    private static String signLicense(String installId) throws Exception {
        // Must match the dev public key embedded in LicenseVerifier
        byte[] pkcs8 = Base64.getDecoder().decode("MC4CAQAwBQYDK2VwBCIEIBIYeDSqPYckDWsZActWNugz9bKG74btoLC88v/Pu8vD");
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(pkcs8));
        String payloadJson = objectMapperStatic().writeValueAsString(java.util.Map.of(
                "product", "leonbookinsKarpathos",
                "licenseId", "test-lic-1",
                "installId", installId,
                "customerName", "Test Customer",
                "customerTelephone", "+30",
                "customerEmail", "test@example.com",
                "issuedAt", Instant.now().toString()
        ));
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(privateKey);
        sig.update(payloadBytes);
        byte[] signature = sig.sign();
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString(payloadBytes) + "." + enc.encodeToString(signature);
    }

    private static ObjectMapper objectMapperStatic() { return new ObjectMapper(); }
}
