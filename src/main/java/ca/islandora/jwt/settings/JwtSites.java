package ca.islandora.jwt.settings;

import java.util.ArrayList;
import java.util.List;

class JwtSites {
    private int version = -1;
    private List<JwtSite> sites = new ArrayList<>();

    void addSite(final JwtSite site) {
        sites.add(site);
    }
    List<JwtSite> getSites() {
        return sites;
    }

    int getVersion() {
        return this.version;
    }
    void setVersion(final int version) {
        this.version = version;
    }
}
