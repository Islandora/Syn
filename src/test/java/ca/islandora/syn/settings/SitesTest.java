package ca.islandora.syn.settings;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SitesTest {

    @Test
    public void TestJwtSites() {
        final Sites sites = new Sites();
        assertEquals(-1, sites.getVersion());
        sites.setVersion(2);
        assertEquals(2,sites.getVersion());
        assertEquals(0, sites.getSites().size());

        final Site site = new Site();
        sites.addSite(site);
        assertEquals(1, sites.getSites().size());
        assertEquals(site, sites.getSites().get(0));
    }

    @Test
    public void TestToken() {
        final Sites sites = new Sites();
        final Token token = new Token();
        assertEquals(0, sites.getTokens().size());
        sites.addToken(token);
        assertEquals(1, sites.getTokens().size());
        assertEquals(token, sites.getTokens().get(0));
    }
}
