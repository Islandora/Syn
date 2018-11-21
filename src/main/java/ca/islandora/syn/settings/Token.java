package ca.islandora.syn.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Token {
    private String user = "islandoraAdmin";
    private final List<String> roles = new ArrayList<>();
    private String value = "";

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
            Collections.addAll(this.roles, parts);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String token) {
        this.value = token.trim();
    }
}