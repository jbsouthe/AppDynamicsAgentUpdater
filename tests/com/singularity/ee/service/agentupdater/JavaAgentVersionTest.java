package com.singularity.ee.service.agentupdater;

import junit.framework.TestCase;
import org.junit.Test;

public class JavaAgentVersionTest extends TestCase {

    public JavaAgentVersionTest() {}

    @Test
    public void testGetVersion() {
        JavaAgentVersion javaAgentVersion = new JavaAgentVersion("22.2.0.3555");
        assert javaAgentVersion.major == 22;
        assert javaAgentVersion.minor == 2;
        assert javaAgentVersion.hotfix == 0;
        assert javaAgentVersion.build == 3555;
        System.out.println("agent dir: "+ javaAgentVersion.getDirectory());
        assert javaAgentVersion.getDirectory().equals("ver22.2.0.3555");
    }


}