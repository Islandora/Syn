package ca.islandora.syn.settings;

import java.util.ArrayList;
import java.util.List;

public class Sites {
    private int version = -1;
    private List<Site> sites = new ArrayList<>();

    public void addSite(final Site site) {
        sites.add(site);
    }
    public List<Site> getSites() {
        return sites;
    }

    public int getVersion() {
        return this.version;
    }
    public void setVersion(final int version) {
        this.version = version;
    }
}
