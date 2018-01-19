package ca.islandora.syn.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TokenTest {
    Token token;

    @Before
    public void initializeSite() {
        this.token = new Token();
    }

    @Test
    public void testTokenUser() {
        assertEquals("islandoraAdmin", this.token.getUser());
        final String testVal = "test";
        this.token.setUser(testVal);
        assertEquals(testVal, this.token.getUser());
    }

    @Test
    public void testTokenRoles() {
        assertEquals(0, this.token.getRoles().size());

        this.token.setRoles("test");
        assertEquals(1, this.token.getRoles().size());
        assertEquals("test", this.token.getRoles().get(0));

        this.token.setRoles("this,is,a,test");
        assertEquals(4, this.token.getRoles().size());
        assertEquals("this", this.token.getRoles().get(0));
        assertEquals("is", this.token.getRoles().get(1));
        assertEquals("a", this.token.getRoles().get(2));
        assertEquals("test", this.token.getRoles().get(3));
    }

    @Test
    public void testTokenToken() {
        assertTrue(this.token.getValue().isEmpty());
        final String testVal = "test";
        this.token.setValue(testVal);
        assertEquals(testVal, this.token.getValue());
        this.token.setValue("   " + testVal);
        assertEquals(testVal, this.token.getValue());
    }
}
