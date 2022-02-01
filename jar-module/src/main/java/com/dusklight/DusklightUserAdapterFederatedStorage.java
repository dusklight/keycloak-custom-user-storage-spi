package com.dusklight;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Allows access to a Dusklight user's data.
 *
 * For simplicity, most of the setters are omitted or not implemented, such as saving attribute values back to the
 * Dusklight database. For production use, consider implementing them as needed.
 *
 * Note that this derives from AbstractUserAdapterFederatedStorage, which means it will support full Keycloak feature-set
 * by using Keycloak's storage for any features not specifically implemented/overriden in this class.
 * Refer to documentation: https://www.keycloak.org/docs/15.0/server_development/#augmenting-external-storage
 */
public class DusklightUserAdapterFederatedStorage extends AbstractUserAdapterFederatedStorage {
    private static final Logger logger = Logger.getLogger(DusklightUserAdapterFederatedStorage.class);

    private final DusklightUserEntity dusklightUserEntity;

    public DusklightUserAdapterFederatedStorage(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel,
                                                DusklightUserEntity DusklightUserEntity) {
        super(session, realm, storageProviderModel);
        this.dusklightUserEntity = DusklightUserEntity;
    }

    @Override
    public String getUsername() {
        return dusklightUserEntity.getUsername();
    }

    @Override
    public void setUsername(String username) {
    }

    public String getPasswordHash() {
        return dusklightUserEntity.getPasswordHash();
    }

    public void setPasswordHash(String passwordHash) {
        dusklightUserEntity.setPasswordHash(passwordHash);
    }

    @Override
    public String getFirstName() {
        return dusklightUserEntity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
    }

    @Override
    public String getLastName() {
        return dusklightUserEntity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
    }

    @Override
    public String getEmail() {
        return null;
    }

    public int getDatabaseUserId() { return dusklightUserEntity.getUserId(); }

    @Override
    public void setEmail(String email) {
    }

    /**
     * Demonstrates exposing custom data as attributes in Keycloak.  In this case, the Department value of the user
     * from the database is exposed as an attribute.
     *
     * @param name
     * @return List of attributes
     */
    @Override
    public List<String> getAttribute(String name) {
        ArrayList<String> attributeValueList = new ArrayList<>();

        if (name.toLowerCase() == "department") {
            attributeValueList.add(dusklightUserEntity.getDepartment());
        } else {
            return super.getAttribute(name);
        }

        return attributeValueList;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        var attributes = super.getAttributes();

        attributes.put("department", getAttribute("department"));

        // Handle Keycloak's built-in FIRST_NAME and LAST_NAME attribute names, otherwise these values will be empty
        // in the User Detail screen.
        var firstName = getFirstName();
        attributes.put(FIRST_NAME, firstName == null ? null : List.of(firstName));

        var lastName = getLastName();
        attributes.put(LAST_NAME, lastName == null ? null : List.of(lastName));

        return attributes;
    }
}
