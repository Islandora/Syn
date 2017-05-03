package ca.islandora.syn.settings;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SiteTest {
    Site site;

    @Before
    public void initializeSite() {
        this.site = new Site();
    }

    @Test
    public void testSiteUrl() {
        assertNull(this.site.getUrl());
        final String testVal = "test";
        this.site.setUrl(testVal);
        assertEquals(testVal, this.site.getUrl());
    }

    @Test
    public void testSiteAlgorithm() {
        assertNull(this.site.getAlgorithm());
        final String testVal = "test";
        this.site.setAlgorithm(testVal);
        assertEquals(testVal, this.site.getAlgorithm());
    }

    @Test
    public void testSiteKey() {
        assertNull(this.site.getKey());
        final String testVal = "test";
        this.site.setKey(testVal);
        assertEquals(testVal, this.site.getKey());
    }

    @Test
    public void testSitePath() {
        assertNull(this.site.getPath());
        final String testVal = "test";
        this.site.setPath(testVal);
        assertEquals(testVal, this.site.getPath());
    }

    @Test
    public void testSiteEncoding() {
        assertNull(this.site.getEncoding());
        final String testVal = "test";
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

    @Test
    public void testSiteAnonymous() {
        assertFalse(this.site.getAnonymous());
        this.site.setAnonymous(true);
        assertTrue(this.site.getAnonymous());
        this.site.setAnonymous(false);
        assertFalse(this.site.getAnonymous());
    }
}
