package ca.islandora.jwt.settings;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JwtSiteTest {
    JwtSite site;

    @Before
    public void initializeSite() {
        this.site = new JwtSite();
    }

    @Test
    public void testSiteUrl() {
        assertNull(this.site.getUrl());
        String testVal = "test";
        this.site.setUrl(testVal);
        assertEquals(testVal, this.site.getUrl());
    }

    @Test
    public void testSiteAlgorithm() {
        assertNull(this.site.getAlgorithm());
        String testVal = "test";
        this.site.setAlgorithm(testVal);
        assertEquals(testVal, this.site.getAlgorithm());
    }

    @Test
    public void testSiteKey() {
        assertNull(this.site.getKey());
        String testVal = "test";
        this.site.setKey(testVal);
        assertEquals(testVal, this.site.getKey());
    }

    @Test
    public void testSitePath() {
        assertNull(this.site.getPath());
        String testVal = "test";
        this.site.setPath(testVal);
        assertEquals(testVal, this.site.getPath());
    }

    @Test
    public void testSiteEncoding() {
        assertNull(this.site.getEncoding());
        String testVal = "test";
        this.site.setEncoding(testVal);
        assertEquals(testVal, this.site.getEncoding());
    }

    @Test
    public void testSiteDefault() {
        assertFalse(this.site.getDefault());
        this.site.setDefault(true);
        assertTrue(this.site.getDefault());
        this.site.setDefault(false);
        assertFalse(this.site.getDefault());
    }
}
