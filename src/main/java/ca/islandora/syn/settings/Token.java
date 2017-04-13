package ca.islandora.syn.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Token {
    private String user = "islandoraAdmin";
    private List<String> roles = new ArrayList<>();
    private String token = "";

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(final String roles) {
        this.roles.clear();
        if (!roles.isEmpty()) {
            final String[] parts = roles.split(",");
            Collections.addAll(this.roles,parts);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token.trim();
    }
}