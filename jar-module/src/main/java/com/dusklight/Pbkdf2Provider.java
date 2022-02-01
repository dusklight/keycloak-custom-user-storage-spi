package com.dusklight;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public final class Pbkdf2Provider {

    private Pbkdf2Provider() {}

    public static String generateHashBase64(String textToHash, String saltBase64, int iterations, int hashSizeBytes, String pbkdf2Algorithm) {
        byte[] saltBytes = Base64.getDecoder().decode(saltBase64);

        KeySpec spec = new PBEKeySpec(textToHash.toCharArray(), saltBytes, iterations, hashSizeBytes * 8);
        SecretKeyFactory factory = getSecretKeyFactory(pbkdf2Algorithm);

        try {
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

            return hashBase64;
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateSaltBase64(int saltSizeBytes) {
        byte[] saltBytes = generateSaltBytes(saltSizeBytes);
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
        return saltBase64;
    }

    private static byte[] generateSaltBytes(int saltSizeBytes) {
        byte[] buffer = new byte[saltSizeBytes];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

    private static SecretKeyFactory getSecretKeyFactory(String pbkdf2Algorithm) {
        try {
            return SecretKeyFactory.getInstance(pbkdf2Algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found: " + pbkdf2Algorithm, e);
        }
    }
}
