package org.mineskin.hiatus;

import java.util.UUID;

public class Account {

    private final UUID uuid;
    private final String email;
    private final String hashedEmailAndToken;
    private final String authHeader;

    public Account(UUID uuid, String email, String token) {
        this.uuid = uuid;
        this.email = email;
        this.hashedEmailAndToken = Util.sha256(email + ":" + token);
        this.authHeader = makeAuthHeader();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    private String makeAuthHeader() {
        String joined = "1:" + this.uuid + ":" + hashedEmailAndToken;
        return Util.base64Encode(joined);
    }

    public String getAuthHeader() {
        return authHeader;
    }

    @Override
    public String toString() {
        return "Account{" +
                "uuid=" + uuid +
                ", hashedEmailAndToken='" + hashedEmailAndToken + '\'' +
                ", authHeader='" + authHeader + '\'' +
                '}';
    }

}
