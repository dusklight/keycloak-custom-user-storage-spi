package com.dusklight;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2ProviderTest {

    @Test
    void generateHashBase64_ShouldGenerateValidHash() {

        // Arrange
        String textToHash = "foobar";
        String saltBase64 = "LiMWfPVrAvYKELcoYFMGBw==";
        int iterations = 27500;
        int hashSizeBytes = 64;
        String pbkdf2Algorithm = "PBKDF2WithHmacSHA256";

        // Act
        String hash = Pbkdf2Provider.generateHashBase64(
                textToHash, saltBase64, iterations, hashSizeBytes, pbkdf2Algorithm);

        // Assert
        assertThat(hash).isEqualTo("kcBJWHhEpQT1PSo4J8ZOkh1r6PLens1OLZw+k09stYL+oaQAzuKIgnK74O5N89BhMWIKZ1t19FJD891pjwHIzg==");
    }

    @Test
    void generateSaltBase64_ShouldGenerateValidSalt() {

        // Act
        String salt = Pbkdf2Provider.generateSaltBase64(16);

        // Assert
        assertThat(salt).isNotNull().hasSizeGreaterThan(1);
    }
}
