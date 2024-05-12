package org.apache.seata.integration.tx.api.remoting.parser;

import org.apache.seata.integration.tx.api.remoting.RemotingParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRemotingParserTest {

    private static final DefaultRemotingParser remotingParser = DefaultRemotingParser.get();

    @BeforeAll
    public static void setUp() {
        remotingParser.registerRemotingParser(new SimpleRemotingParser());
    }

    @Test
    public void testIsRemoting() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        RemotingParser parser = remotingParser.isRemoting(remoteBean, remoteBean.getClass().getName());
        assertInstanceOf(SimpleRemotingParser.class, parser);
    }

    @Test
    public void testIsRemotingFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertNull(remotingParser.isRemoting(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsReference() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        assertTrue(remotingParser.isReference(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsReferenceFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertFalse(remotingParser.isReference(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromObject() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        assertTrue(remotingParser.isService(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromObjectFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertFalse(remotingParser.isService(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromClass() {
        assertTrue(remotingParser.isService(SimpleRemoteBean.class));
    }

    @Test
    public void testIsServiceFromClassFail() {
        assertFalse(remotingParser.isService(SimpleBean.class));
    }
}
