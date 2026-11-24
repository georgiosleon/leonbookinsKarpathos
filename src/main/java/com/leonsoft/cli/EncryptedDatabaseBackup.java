package com.leonsoft.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class EncryptedDatabaseBackup {
    private static final byte[] MAGIC = "LBKENC01".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int SALT_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int ITERATIONS = 600_000;
    private static final int MIN_PASSPHRASE_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private EncryptedDatabaseBackup() {
    }

    static boolean isEncrypted(Path backup) throws IOException {
        try (InputStream input = Files.newInputStream(backup)) {
            return Arrays.equals(input.readNBytes(MAGIC.length), MAGIC);
        }
    }

    static void encrypt(Path source, Path destination, char... passphrase) throws IOException {
        validatePassphrase(passphrase);
        byte[] salt = new byte[SALT_LENGTH];
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);
        byte[] key = deriveKey(passphrase, salt);
        try (OutputStream output = Files.newOutputStream(destination,
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            output.write(MAGIC);
            output.write(salt);
            output.write(nonce);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce);
            try (CipherOutputStream encrypted = new CipherOutputStream(output, cipher);
                 InputStream input = Files.newInputStream(source)) {
                input.transferTo(encrypted);
            }
        } finally {
            Arrays.fill(key, (byte) 0);
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
        }
    }

    static void decrypt(Path source, Path destination, char... passphrase) throws IOException {
        validatePassphrase(passphrase);
        byte[] key = null;
        boolean decryptedSuccessfully = false;
        try (InputStream input = Files.newInputStream(source)) {
            byte[] magic = input.readNBytes(MAGIC.length);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IOException("File is not a supported encrypted backup");
            }
            byte[] salt = input.readNBytes(SALT_LENGTH);
            byte[] nonce = input.readNBytes(NONCE_LENGTH);
            if (salt.length != SALT_LENGTH || nonce.length != NONCE_LENGTH) {
                throw new IOException("Encrypted backup header is incomplete");
            }
            key = deriveKey(passphrase, salt);
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, key, nonce);
            try (OutputStream output = Files.newOutputStream(destination,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int count = input.read(buffer);
                while (count != -1) {
                    byte[] plaintext = cipher.update(buffer, 0, count);
                    if (plaintext != null) {
                        output.write(plaintext);
                        Arrays.fill(plaintext, (byte) 0);
                    }
                    count = input.read(buffer);
                }
                byte[] finalPlaintext = cipher.doFinal();
                try {
                    output.write(finalPlaintext);
                } finally {
                    Arrays.fill(finalPlaintext, (byte) 0);
                }
                decryptedSuccessfully = true;
            } finally {
                Arrays.fill(salt, (byte) 0);
                Arrays.fill(nonce, (byte) 0);
            }
        } catch (GeneralSecurityException error) {
            throw new IOException(
                    "Could not decrypt backup; the passphrase may be wrong or the file may be damaged",
                    error);
        } catch (IOException error) {
            throw new IOException(
                    "Could not decrypt backup; the passphrase may be wrong or the file may be damaged",
                    error);
        } finally {
            if (!decryptedSuccessfully) {
                Files.deleteIfExists(destination);
            }
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
        }
    }

    private static byte[] deriveKey(char[] passphrase, byte[] salt) {
        PBEKeySpec keySpec = new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Required backup encryption is unavailable", error);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static Cipher cipher(int operation, byte[] key, byte[] nonce) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(operation, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return cipher;
        } catch (GeneralSecurityException error) {
            throw new IOException("Could not initialize encrypted backup", error);
        }
    }

    private static void validatePassphrase(char... passphrase) {
        if (passphrase == null || passphrase.length < MIN_PASSPHRASE_LENGTH) {
            throw new IllegalArgumentException(
                    "Backup passphrase must be at least " + MIN_PASSPHRASE_LENGTH + " characters");
        }
    }
}
