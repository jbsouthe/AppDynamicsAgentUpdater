package com.singularity.ee.service.agentupdater;

import com.singularity.ee.agent.appagent.kernel.spi.data.IServiceConfig;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.util.string.StringOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

public class AgentNodeProperties extends Observable {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.AgentNodeProperties");
    public static final String[] NODE_PROPERTIES = new String[]{
            "agent.upgrader.enabled", "agent.upgrader.version.max","agent.upgrader.version.min",
            "agent.upgrader.version.preferred","agent.upgrader.version.current", "agent.upgrader.repo.url"};
    private final Map<String, String> properties = new HashMap<>();

    public void initializeConfigs(IServiceConfig serviceConfig) {
        Map configProperties = serviceConfig.getConfigProperties();
        if( configProperties != null ) {
            boolean enabled = StringOperations.safeParseBoolean((String)((String)configProperties.get("agent.upgrader.enabled")), (boolean)true);
            this.properties.put("agent.upgrader.enabled", Boolean.toString(enabled));
            this.properties.put("agent.upgrader.version.max", (String)configProperties.get("agent.upgrader.version.max"));
            this.properties.put("agent.upgrader.version.min", (String)configProperties.get("agent.upgrader.version.min"));
            this.properties.put("agent.upgrader.version.preferred", (String)configProperties.get("agent.upgrader.version.preferred"));
            this.properties.put("agent.upgrader.version.current", (String)configProperties.get("agent.upgrader.version.current"));
            this.properties.put("agent.upgrader.repo.url", (String)configProperties.get("agent.upgrader.repo.url"));
            logger.info("Initializing the properties " + this);
        } else {
            logger.error("Config properties map is null?!?!");
        }
    }

    public String getProperty( String name ) {
        return this.properties.get(name);
    }

    public void updateProperty( String name, String value ) {
        String existingPropertyValue = this.properties.get(name);
        if( !StringOperations.isEmpty((String)value) && !value.equals(existingPropertyValue)) {
            this.properties.put(name, value);
            logger.info("updated property = " + name + " with value = " + value);
            this.notifyMonitoringService(name);
        } else {
            logger.info("did not update property = " + name + " because it was either unchanged or empty");
        }
    }

    protected void notifyMonitoringService(String name) {
        this.setChanged();
        this.notifyObservers(name);
    }

    public String toString() {
        return "AgentNodeProperties{properties=" + this.properties + '}';
    }

    public boolean isEnabled() {
        return StringOperations.safeParseBoolean((String)this.getProperty("agent.upgrader.enabled"), (boolean)true);
    }

    public boolean isMaxVersionSet() { return this.properties.get("agent.upgrader.version.max") != null; }
    public boolean isMinVersionSet() { return this.properties.get("agent.upgrader.version.min") != null; }
    public boolean isPreferredVersionSet() { return this.properties.get("agent.upgrader.version.preferred") != null; }
    public boolean isCurrentVersionSet() { return this.properties.get("agent.upgrader.version.current") != null; }

    public JavaAgentVersion getMinVersion() {
        if( isMinVersionSet() ) return new JavaAgentVersion(this.properties.get("agent.upgrader.version.min"));
        return null;
    }

    public JavaAgentVersion getMaxVersion() {
        if( isMaxVersionSet() ) return new JavaAgentVersion(this.properties.get("agent.upgrader.version.max"));
        return null;
    }

    public JavaAgentVersion getPreferredVersion() {
        if( isPreferredVersionSet() ) return new JavaAgentVersion(this.properties.get("agent.upgrader.version.preferred"));
        return null;
    }

    public JavaAgentVersion getCurrentVersion() {
        if( isCurrentVersionSet() ) return new JavaAgentVersion(this.properties.get("agent.upgrader.version.current"));
        return null;
    }

    public String getDownloadURL() {
        return this.properties.get("agent.upgrader.repo.url");
    }
}
