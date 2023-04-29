package com.singularity.ee.service.agentupdater.web;

import com.singularity.ee.service.agentupdater.AgentNodeProperties;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SimpleFileServerTest extends TestCase {

    private SimpleFileServer simpleFileServer;
    private AgentNodeProperties agentNodeProperties;
    public SimpleFileServerTest() {

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.simpleFileServer = new SimpleFileServer(8989, new File("./tests/resources") );
        this.agentNodeProperties = new AgentNodeProperties();
        this.agentNodeProperties.updateProperty("agent.upgrader.repo.url", "http://localhost:8989");
    }

    @Test
    public void testSimpleFileServer() throws IOException {
        AgentDownloader agentDownloader = new AgentDownloader("java-jdk8", "22", agentNodeProperties);
        assert agentDownloader != null;
        ZipFileWithVersion zipFileWithVersion = agentDownloader.getAgentZipFile();
    }
}