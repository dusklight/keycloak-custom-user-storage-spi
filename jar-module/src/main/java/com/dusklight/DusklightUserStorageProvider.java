package com.dusklight;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class DusklightUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserQueryProvider
{
    private static final Logger logger = Logger.getLogger(DusklightUserStorageProvider.class);

    // These values are the same default values used by Keycloak.
    private final String pbkdf2Algorithm = "PBKDF2WithHmacSHA256";
    private final int pbkdf2DefaultIterations = 27500;
    private final int pbkdf2DefaultSaltSizeBytes = 16;
    private final int pbkdf2DefaultHashSizeBytes = 64;

    private final KeycloakSession session;
    private final ComponentModel model;
    private final String connectionString;

    // Cached users that were found in a Keycloak transaction (recommended by Keycloak documentation).
    protected Map<String, DusklightUserAdapterFederatedStorage> instanceCachedUsers = new HashMap<>();

    public DusklightUserStorageProvider(KeycloakSession session, ComponentModel model, String connectionString) {
        this.session = session;
        this.model = model;
        this.connectionString = connectionString;
    }

    //region UserStorageProvider implementation

    // Refer to https://www.keycloak.org/docs/15.0/server_development/#provider-interfaces

    @Override
    public void close() {}

    //endregion

    //region UserLookupProvider implementation

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        DusklightUserAdapterFederatedStorage cachedUser = instanceCachedUsers.get(username);

        if (cachedUser == null) {
            var users = retrieveDusklightUsers(username, false);

            if (users.size() != 0) {
                cachedUser = new DusklightUserAdapterFederatedStorage(session, realm, model, users.get(0));
                instanceCachedUsers.put(username, cachedUser);

                return cachedUser;
            }
            else {
                // User was not found in the database.
                return null;
            }
        } else {
            // User already in the instanceCache
            return cachedUser;
        }
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        var storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String s, RealmModel realmModel) {
        return null;
    }

    //endregion

    //region CredentialInputValidator implementation

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    /**
     * Validates password, stored in the following format in the database: "hash.salt.iterations"
     *
     * @param realm realm
     * @param user user
     * @param input input
     * @return true if valid, false otherwise.
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        if (user == null) {
            return false;
        }

        DusklightUserAdapterFederatedStorage dusklightUser = getDusklightUserAdapter(user);

        String passwordHash = dusklightUser.getPasswordHash();
        String[] passwordParts = passwordHash.split("\\.");

        if (passwordParts.length != 3) {
            return false;
        }

        try {
            String hash = passwordParts[0];
            String salt = passwordParts[1];
            int iterations = Integer.parseInt(passwordParts[2]);

            UserCredentialModel cred = (UserCredentialModel)input;
            String userPassword = cred.getValue();

            String computedHash = Pbkdf2Provider.generateHashBase64(userPassword,
                    salt, iterations, pbkdf2DefaultHashSizeBytes, pbkdf2Algorithm);

            return computedHash.equals(hash);
        } catch (Exception ex) {
            logger.error("isValid: Error occurred while validating password: " + dusklightUser.getId(), ex);
        }

        return false;
    }

    //endregion

    //region CredentialInputUpdater implementation

    /**
     * Updates the user's password in the database and in the user object.
     * The password is stored as hash, in the following format: "hash.salt.iterations"
     *
     * @param realm realm
     * @param user user
     * @param input input
     * @return True if the password was updated successfully, otherwise false.
     */
    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        if (user == null) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel)input;
        String userPassword = cred.getValue();

        String salt = Pbkdf2Provider.generateSaltBase64(pbkdf2DefaultSaltSizeBytes);
        String hash = Pbkdf2Provider.generateHashBase64(userPassword,
                salt, pbkdf2DefaultIterations, pbkdf2DefaultHashSizeBytes, pbkdf2Algorithm);

        String passwordHash = hash + "." + salt + "." + pbkdf2DefaultIterations;

        DusklightUserAdapterFederatedStorage dusklightUser = getDusklightUserAdapter(user);

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            int count = new QueryRunner()
                    .update(connection, "UPDATE Users SET PasswordHash = ? WHERE UserId = ? ", passwordHash, dusklightUser.getDatabaseUserId());

            if (count == 1) {
                dusklightUser.setPasswordHash(passwordHash);

                return true;
            } else {
                logger.error("updateCredential: More than one user found.  Row count: " + count);
                return false;
            }
        } catch (SQLException ex) {
            logger.error("updateCredential: SQL Exception: ", ex);
        } catch (Exception ex) {
            logger.error("updateCredential: Exception occurred: ", ex);
        }

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {
        logger.warn("disableCredentialType not implemented.");
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realmModel, UserModel userModel) {
        return Collections.EMPTY_SET;
    }

    //endregion

    //region UserQueryProvider implementation

    @Override
    public int getUsersCount(RealmModel realmModel) {
        int count;

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            count = new QueryRunner()
                    .query(connection, "SELECT Count(1) FROM Users", new ScalarHandler<>());

        } catch (SQLException ex) {
            logger.error("getUserCount: SQL Exception:", ex);
            return 0;
        }

        return count;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realmModel) {
        List<UserModel> userModelList = new ArrayList<>();

        var dusklightUserEntities = retrieveDusklightUsers(null, false);

        dusklightUserEntities.forEach(
                userEntity -> userModelList.add(new DusklightUserAdapterFederatedStorage(session, realmModel, model, userEntity)));

        return userModelList;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realmModel, int firstResult, int maxResult) {
        var users = getUsers(realmModel);
        return users.stream()
                .skip(firstResult)
                .limit(maxResult)
                .collect(toList());
    }

    /**
     * Searches Dusklight database for users matching the username for the search parameter.
     *
     * Note that according to https://www.keycloak.org/docs/latest/server_development/index.html#implementing-userqueryprovider,
     * the search parameter can be either username or email.  For simplicity, this only searches by username since the demo
     * database doesn't have email.
     *
     * @param search Search term
     * @param realmModel Realm model
     * @return List of users
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realmModel) {
        List<UserModel> userModelList = new ArrayList<>();

        var dusklightUserEntities = retrieveDusklightUsers(search, true);

        dusklightUserEntities.forEach(
                userEntity -> userModelList.add(new DusklightUserAdapterFederatedStorage(session, realmModel, model, userEntity)));

        return userModelList;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realmModel, int firstResult, int maxResult) {
        var users = searchForUser(search, realmModel);
        return users.stream()
                .skip(firstResult)
                .limit(maxResult)
                .collect(toList());
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realmModel) {
        if (params != null && params.size() == 0) {
            return getUsers(realmModel);
        }

        var usernameSearchString = (params == null ? null : params.get("username"));
        if (usernameSearchString == null) return Collections.EMPTY_LIST;

        return searchForUser(usernameSearchString, realmModel);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realmModel, int firstResult, int maxResult) {
        if (params == null || params.isEmpty()) {
            // It seems Keycloak v15 is not calling getUsers() methods when "View All Users" button is clicked (though in
            // v12, it did), instead, it's calling this method.  See https://issues.redhat.com/browse/KEYCLOAK-12988
            return getUsers(realmModel, firstResult, maxResult);
        }

        var usernameSearchString = params.get("username");
        if (usernameSearchString == null) return Collections.EMPTY_LIST;

        return searchForUser(usernameSearchString, realmModel, firstResult, maxResult);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realmModel, GroupModel groupModel) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realmModel, GroupModel groupModel, int firstResult, int maxResult) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attributeName, String attributeValue, RealmModel realmModel) {
        return Collections.EMPTY_LIST;
    }

    //endregion

    /**
     * Retrieves the DusklightUserAdapter with the logic to handle cases when Keycloak's cache is enabled for the federation.
     *
     * @param user UserModel that contains, or is the same type as, DusklightUserAdapterFederatedStorage
     * @return DusklightUserAdapterFederatedStorage from the user parameter.
     */
    public DusklightUserAdapterFederatedStorage getDusklightUserAdapter(UserModel user) {
        DusklightUserAdapterFederatedStorage dusklightUserAdapter;

        if (user instanceof CachedUserModel) {
            dusklightUserAdapter = (DusklightUserAdapterFederatedStorage)((CachedUserModel)user).getDelegateForUpdate();
        } else {
            dusklightUserAdapter = (DusklightUserAdapterFederatedStorage)user;
        }

        return dusklightUserAdapter;
    }

    /**
     * Looks for users in the Dusklight database.
     *
     * @param optionalUsername If not null, only the user matching the username will be returned.
     * @param useLike If Username is passed in, this value must be passed in.  True if the query should use the LIKE
     *                operator, false if it should use the = operator.
     * @return List of DusklightUserEntity. If none found, an empty List.
     */
    @NotNull
    protected List<DusklightUserEntity> retrieveDusklightUsers(String optionalUsername, Boolean useLike) {
        // TODO: Maybe refactor to two separate methods - one for retrieving all, and one for specific user.

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            QueryRunner runner = new QueryRunner();
            List<DusklightUserEntity> users;

            // Text blocks not supported yet in Java 11.
            String sql = "" +
                    "SELECT\n" +
                    "   UserId, Username, PasswordHash, FirstName, LastName, Department\n" +
                    "FROM\n" +
                    "   Users\n";

            if (optionalUsername != null) {
                if (useLike) {
                    sql += "WHERE Username LIKE ?";

                    users = runner.query(connection, sql,
                            new BeanListHandler<>(DusklightUserEntity.class),
                            "%" + optionalUsername + "%");
                } else {
                    sql += "WHERE Username = ?";

                    users = runner.query(connection, sql,
                            new BeanListHandler<>(DusklightUserEntity.class),
                            optionalUsername);
                }
            } else {
                users = runner.query(connection, sql,
                        new BeanListHandler<>(DusklightUserEntity.class));
            }

            return users;

        } catch (SQLException ex) {
            logger.error("retrieveDusklightUsers: SQL Exception:", ex);
            return new ArrayList<DusklightUserEntity>();
        }
    }
}
