package ca.islandora.syn.settings;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

public class Config {
    private int version = -1;
    private final List<Site> sites = new ArrayList<>();
    private final List<Token> tokens = new ArrayList<>();

    @JsonSetter("site")
    public void addSite(final Site site) {
        this.sites.add(site);
    }

    public List<Site> getSites() {
        return this.sites;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    @JsonSetter("token")
    public void addToken(final Token token) {
        this.tokens.add(token);
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

}
