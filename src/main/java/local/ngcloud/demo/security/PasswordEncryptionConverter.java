package local.ngcloud.demo.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Converter
@Component
public class PasswordEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static String secretKey;

    // Injection de la clé via Spring, mais assignée à un champ statique pour le convertisseur JPA
    @Value("${encryption.host.key:my-default-secret-key-12345}")
    public void setSecretKey(String key) {
        secretKey = key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec());
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chiffrement du mot de passe", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec());
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du déchiffrement du mot de passe", e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        // On s'assure que la clé fait 16 octets pour l'AES-128
        byte[] keyBytes = new byte[16];
        byte[] originalKeyBytes = secretKey.getBytes();
        int len = Math.min(originalKeyBytes.length, keyBytes.length);
        System.arraycopy(originalKeyBytes, 0, keyBytes, 0, len);
        return new SecretKeySpec(keyBytes, "AES");
    }
}