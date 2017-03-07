package ca.islandora.jwt.settings;

import java.util.ArrayList;
import java.util.List;

public class JwtSites {
    private int version = -1;
    private List<JwtSite> sites = new ArrayList<>();

    public void addSite(JwtSite site) {
        sites.add(site);
    }
    public List<JwtSite> getSites() {
        return sites;
    }

    public int getVersion() {
        return this.version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
}
