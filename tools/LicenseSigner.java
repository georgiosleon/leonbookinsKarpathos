import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

public class LicenseSigner {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: java --source 25 tools/LicenseSigner.java <privateKeyPkcs8Base64File> <installId> <customerName> <customerTelephone> <customerEmail>");
            System.exit(2);
        }
        String privateKeyFile = args[0];
        String installId = args[1];
        String customerName = args[2];
        String customerTelephone = args[3];
        String customerEmail = args[4];

        String privateKeyB64 = Files.readString(Path.of(privateKeyFile)).trim();
        byte[] pkcs8 = Base64.getDecoder().decode(privateKeyB64);
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));

        String payloadJson = "{"
                + "\"product\":\"leonbookinsKarpathos\","
                + "\"licenseId\":\"" + escape(UUID.randomUUID().toString()) + "\","
                + "\"installId\":\"" + escape(installId) + "\","
                + "\"customerName\":\"" + escape(customerName) + "\","
                + "\"customerTelephone\":\"" + escape(customerTelephone) + "\","
                + "\"customerEmail\":\"" + escape(customerEmail) + "\","
                + "\"issuedAt\":\"" + escape(LocalDate.now().toString()) + "\""
                + "}";
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(payloadBytes);
        byte[] sig = signature.sign();

        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        System.out.println(key);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
