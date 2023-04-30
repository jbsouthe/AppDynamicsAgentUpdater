package com.singularity.ee.service.agentupdater;

import com.singularity.ee.agent.appagent.kernel.spi.IServicePropertyListener;

public class AgentNodePropertyListener implements IServicePropertyListener {
    private VersionSelfMonitoringService versionSelfMonitoringService;

    public AgentNodePropertyListener(VersionSelfMonitoringService versionSelfMonitoringService) {
        this.versionSelfMonitoringService=versionSelfMonitoringService;
        this.versionSelfMonitoringService.getServiceContext().getKernel().getConfigManager().registerConfigPropertyChangeListener("DynamicService", AgentNodeProperties.NODE_PROPERTIES, (IServicePropertyListener)this);
    }

    @Override
    public void servicePropertyChanged(String serviceName, String propertyName, String newPropertyValue) {
        this.versionSelfMonitoringService.getAgentNodeProperties().updateProperty(propertyName, newPropertyValue);
    }
}
