package com.dusklight;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

public class DusklightUserStorageProviderFactory implements UserStorageProviderFactory<DusklightUserStorageProvider> {
    private static final Logger logger = Logger.getLogger(DusklightUserStorageProviderFactory.class);

    // This is the name used in Keycloak's Federation Provider drop-down list.
    public static final String PROVIDER_NAME = "dusklight-database";

    public static final String CONFIG_DB_SERVER = "dusklight_db_server";
    public static final String CONFIG_DB_INSTANCE_NAME = "dusklight_db_instance_name";
    public static final String CONFIG_DB_PORT = "dusklight_db_port";
    public static final String CONFIG_DB_DATABASE_NAME = "dusklight_db_name";
    public static final String CONFIG_DB_USE_INTEGRATED_SECURITY = "dusklight_db_use_integrated_security";
    public static final String CONFIG_DB_USERNAME = "dusklight_db_username";
    public static final String CONFIG_DB_PASSWORD = "dusklight_db_password";

    protected static final String SQLSERVER_APPLICATION_NAME = "DusklightKeycloakProvider";

    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(CONFIG_DB_SERVER)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Server Host Name or IP")
                .defaultValue("")
                .helpText("The SQL Server to connect to.")
                .add()

                .property().name(CONFIG_DB_PORT)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Host Server Port")
                .defaultValue("")
                .helpText("The port number of the database server to use.  Leave blank to use the default.")
                .add()

                .property().name(CONFIG_DB_INSTANCE_NAME)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Instance Name")
                .defaultValue("")
                .helpText("The database instance name on the server.  Leave blank if the default instance should be used.")
                .add()

                .property().name(CONFIG_DB_DATABASE_NAME)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Name")
                .defaultValue("")
                .helpText("The database name on the server.")
                .add()

                .property().name(CONFIG_DB_USE_INTEGRATED_SECURITY)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Use Integrated Security")
                .defaultValue(Boolean.FALSE)
                .helpText("If set to on, integrated security will be used and the username and password specified below will be ignored.")
                .add()

                .property().name(CONFIG_DB_USERNAME)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Username")
                .defaultValue("")
                .helpText("The username to use for the database connection.")
                .add()

                // TODO: Note that this is not stored securely in the Keycloak database.
                .property().name(CONFIG_DB_PASSWORD)
                .type(ProviderConfigProperty.PASSWORD)
                .label("Password")
                .defaultValue("")
                .helpText("The password to use for the database connection.")
                .add()

                .build();
    }

    @Override
    public DusklightUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        var connectionString = buildDatabaseConnectionString(model);

        return new DusklightUserStorageProvider(session, model, connectionString);
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        UserStorageProviderFactory.super.validateConfiguration(session, realm, config);

        // TODO: Consider attempting to connect to the database here and log or throw exception if can't connect,
        //  keeping in mind that the database server might not be up and running yet when Keycloak starts.
        //  See: https://www.keycloak.org/docs/15.0/server_development/index.html#configuration-example
    }

    /**
     * Builds the JDBC MSSQL Server database connection string based on the config.
     *
     * @param config config ComponentModel
     * @return JDBC Database connection string
     */
    protected String buildDatabaseConnectionString(ComponentModel config) {
        StringBuilder connectionString = new StringBuilder();

        connectionString.append("jdbc:sqlserver://");
        connectionString.append(config.getConfig().getFirst(CONFIG_DB_SERVER));

        var port = config.getConfig().getFirst(CONFIG_DB_PORT);

        if (port != null && !port.isBlank()) {
            connectionString.append(":");
            connectionString.append(port);
        }

        var instanceName = config.getConfig().getFirst(CONFIG_DB_INSTANCE_NAME);

        if (instanceName != null && !instanceName.isBlank()){
            connectionString.append(";instanceName=");
            connectionString.append(instanceName);
        }

        connectionString.append(";databaseName=");
        connectionString.append(config.getConfig().getFirst(CONFIG_DB_DATABASE_NAME));

        var useIntegratedSecurity = Boolean.parseBoolean(
                config.getConfig().getFirst(CONFIG_DB_USE_INTEGRATED_SECURITY));

        if (useIntegratedSecurity) {
            connectionString.append(";integratedSecurity=true");
        } else {
            connectionString.append(";user=");
            connectionString.append(config.getConfig().getFirst(CONFIG_DB_USERNAME));
            connectionString.append(";password=");
            connectionString.append(config.getConfig().getFirst(CONFIG_DB_PASSWORD));
        }

        connectionString.append(";applicationName=");
        connectionString.append(SQLSERVER_APPLICATION_NAME); // Useful for running SQL Trace

        return connectionString.toString();
    }
}
