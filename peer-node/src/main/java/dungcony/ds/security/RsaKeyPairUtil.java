package dungcony.ds.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public final class RsaKeyPairUtil {
    public static final String KEY_ALGORITHM = "RSA";
    public static final int KEY_SIZE_BITS = 2048;
    public static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String MESSAGE_ALGORITHM = "RSA-OAEP-SHA256";

    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private RsaKeyPairUtil() {
    }

    public static PeerKeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE_BITS);
            KeyPair keyPair = generator.generateKeyPair();
            return new PeerKeyPair(
                    encodePublicKey(keyPair.getPublic()),
                    encodePrivateKey(keyPair.getPrivate())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate RSA key pair", e);
        }
    }

    public static PeerKeyPair resolveOrGenerate(String publicKey, String privateKey) {
        if (isUsableKeyPair(publicKey, privateKey)) {
            return new PeerKeyPair(publicKey.trim(), privateKey.trim());
        }
        log.info("Generating a new RSA key pair for peer profile.");
        return generateKeyPair();
    }

    public static PublicKey decodePublicKey(String publicKey) {
        try {
            byte[] bytes = Base64.getDecoder().decode(publicKey);
            return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid public key", e);
        }
    }

    public static PrivateKey decodePrivateKey(String privateKey) {
        try {
            byte[] bytes = Base64.getDecoder().decode(privateKey);
            return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid private key", e);
        }
    }

    public static Cipher newCipher(int mode, java.security.Key key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(mode, key, OAEP_SHA256);
            return cipher;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize RSA cipher", e);
        }
    }

    public static boolean isUsableKeyPair(String publicKey, String privateKey) {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            return false;
        }
        try {
            PublicKey decodedPublicKey = decodePublicKey(publicKey.trim());
            PrivateKey decodedPrivateKey = decodePrivateKey(privateKey.trim());
            byte[] sample = "key-check".getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = newCipher(Cipher.ENCRYPT_MODE, decodedPublicKey).doFinal(sample);
            byte[] decrypted = newCipher(Cipher.DECRYPT_MODE, decodedPrivateKey).doFinal(encrypted);
            return java.util.Arrays.equals(sample, decrypted);
        } catch (Exception e) {
            log.warn("Existing RSA key pair is invalid. It will be regenerated. cause={}", e.getMessage());
            return false;
        }
    }

    private static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private static String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}
