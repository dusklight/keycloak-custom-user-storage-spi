package com.dusklight;

public class DusklightUserEntity {
    private int userId;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String department;

    public DusklightUserEntity() {}

    /**
     * The unique ID of the user in the database.
     *
     * @return Unique ID from the database.
     */
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Note that by default, Keycloak will assume this is unique, as it will append this username to the ID of the
     * federated User Storage SPI as the Keycloak's "universal" ID.
     * Refer to: https://github.com/keycloak/keycloak/blob/15.0.2/server-spi/src/main/java/org/keycloak/storage/adapter/AbstractUserAdapterFederatedStorage.java#L293
     *
     * @return Unique username
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
