package com.dusklight;

import static com.dusklight.DusklightUserStorageProviderFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;

class DusklightUserStorageProviderFactoryTest {

    // TODO: Probably should add more tests, and there seems to be some integration tests here as well:
    // - https://github.com/keycloak/keycloak-quickstarts/blob/15.0.2/user-storage-jpa/src/test/java/org/keycloak/quickstart/ArquillianJpaStorageTest.java
    // Also see:
    // - https://github.com/keycloak/keycloak/tree/15.0.2/testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite/federation/storage

    @Test
    void buildDatabaseConnectionString_NoOptionalSettings_GenerateValidSqlConnectionString() {

        // Arrange
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

        config.add(CONFIG_DB_SERVER, "localhost");
        config.add(CONFIG_DB_DATABASE_NAME, "dbname1");
        config.add(CONFIG_DB_USERNAME, "user1");
        config.add(CONFIG_DB_PASSWORD, "password1");

        componentModel.setConfig(config);

        DusklightUserStorageProviderFactory factory = new DusklightUserStorageProviderFactory();

        // Act
        String connectionString = factory.buildDatabaseConnectionString(componentModel);

        // Assert
        assertThat(connectionString).isEqualTo("jdbc:sqlserver://localhost;databaseName=dbname1;user=user1;password=password1;applicationName=DusklightKeycloakProvider");
    }

    @Test
    void buildDatabaseConnectionString_WithIntegratedSecurity_GenerateValidSqlConnectionString() {

        // Arrange
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

        config.add(CONFIG_DB_SERVER, "localhost");
        config.add(CONFIG_DB_DATABASE_NAME, "dbname1");
        config.add(CONFIG_DB_USE_INTEGRATED_SECURITY, "true");
        // These can be removed, but testing that they will be ignored.
        config.add(CONFIG_DB_USERNAME, "user1");
        config.add(CONFIG_DB_PASSWORD, "password1");

        componentModel.setConfig(config);

        DusklightUserStorageProviderFactory factory = new DusklightUserStorageProviderFactory();

        // Act
        String connectionString = factory.buildDatabaseConnectionString(componentModel);

        // Assert
        assertThat(connectionString).isEqualTo("jdbc:sqlserver://localhost;databaseName=dbname1;integratedSecurity=true;applicationName=DusklightKeycloakProvider");
    }

    @Test
    void buildDatabaseConnectionString_WithOptionalSettings_GenerateValidSqlConnectionString() {

        // Arrange
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

        config.add(CONFIG_DB_SERVER, "localhost");
        config.add(CONFIG_DB_PORT, "1433");
        config.add(CONFIG_DB_INSTANCE_NAME, "instance1");
        config.add(CONFIG_DB_DATABASE_NAME, "dbname1");
        config.add(CONFIG_DB_USERNAME, "user1");
        config.add(CONFIG_DB_PASSWORD, "password1");

        componentModel.setConfig(config);

        DusklightUserStorageProviderFactory factory = new DusklightUserStorageProviderFactory();

        // Act
        String connectionString = factory.buildDatabaseConnectionString(componentModel);

        // Assert
        assertThat(connectionString).isEqualTo("jdbc:sqlserver://localhost:1433;instanceName=instance1;databaseName=dbname1;user=user1;password=password1;applicationName=DusklightKeycloakProvider");
    }
}
