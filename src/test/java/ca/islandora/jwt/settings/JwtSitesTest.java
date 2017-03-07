package ca.islandora.jwt.settings;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JwtSitesTest {

    @Test
    public void TestJwtSites() {
        final JwtSites sites = new JwtSites();
        assertEquals(-1, sites.getVersion());
        sites.setVersion(2);
        assertEquals(2,sites.getVersion());
        assertEquals(0, sites.getSites().size());

        final JwtSite site = new JwtSite();
        sites.addSite(site);
        assertEquals(1, sites.getSites().size());
        assertEquals(site, sites.getSites().get(0));
    }
}
