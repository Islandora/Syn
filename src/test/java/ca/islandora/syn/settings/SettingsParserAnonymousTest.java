package ca.islandora.syn.settings;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

public class SettingsParserAnonymousTest {

    @Test
    public void testSiteAnonymousOn() throws Exception {
        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='http://test.com' algorithm='RS256' encoding='plain' anonymous='true'>"
            , "   test data"
            , "  </site>"
            , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String, Boolean> anonymous = SettingsParser.getSiteAllowAnonymous(SettingsParser.getSites(stream));
        assertEquals(1, anonymous.size());
        assertEquals(true, anonymous.containsKey("http://test.com"));
        assertEquals(true, anonymous.get("http://test.com"));
    }

    @Test
    public void testSiteMultipleAnonymousTest() throws Exception {
        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='http://test.com' algorithm='RS256' encoding='plain' anonymous='true'>"
            , "   test data"
            , "  </site>"
            , "  <site url='http://test2.com' algorithm='RS256' encoding='plain' anonymous='false'>"
            , "   test data"
            , "  </site>"
            , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String, Boolean> anonymous = SettingsParser.getSiteAllowAnonymous(SettingsParser.getSites(stream));
        assertEquals(2, anonymous.size());
        assertEquals(true, anonymous.containsKey("http://test.com"));
        assertEquals(true, anonymous.get("http://test.com"));
        assertEquals(true, anonymous.containsKey("http://test2.com"));
        assertEquals(false, anonymous.get("http://test2.com"));
    }

    @Test
    public void testDefaultMultipleAnonymousTest() throws Exception {
        final String testXml = String.join("\n"
            , "<config version='1'>"
            , "  <site url='http://test.com' algorithm='RS256' encoding='plain' anonymous='true'>"
            , "   test data"
            , "  </site>"
            , "  <site url='http://test2.com' algorithm='RS256' encoding='plain' anonymous='false'>"
            , "   test data"
            , "  </site>"
            , "  <site algorithm='RS256' encoding='plain' anonymous='true' default='true'/>"
            , "</config>"
        );

        final InputStream stream = new ByteArrayInputStream(testXml.getBytes());
        final Map<String, Boolean> anonymous = SettingsParser.getSiteAllowAnonymous(SettingsParser.getSites(stream));
        assertEquals(3, anonymous.size());
        assertEquals(true, anonymous.containsKey("http://test.com"));
        assertEquals(true, anonymous.get("http://test.com"));
        assertEquals(true, anonymous.containsKey("http://test2.com"));
        assertEquals(false, anonymous.get("http://test2.com"));
        assertEquals(true, anonymous.containsKey("default"));
        assertEquals(true, anonymous.get("default"));
    }
}
