# AppDynamics agent upgrade dynamic service extension

[![published](https://static.production.devnetcloud.com/codeexchange/assets/images/devnet-published.svg)](https://developer.cisco.com/codeexchange/github/repo/jbsouthe/AppDynamicsAgentUpdater)

Some setup. This should be installed in the <agent install dir>/ver22.###/external-services/agent-updater directory
the <agent intall dir>/ver22.###/conf/app-agent-config.xml has to have signing disabled:

    <configuration-properties>
        <property name="external-service-directory" value="external-services"/>
        <property name="enable-jar-signing" value="false"/>
    </configuration-properties>


Agents now will be downloaded from our download site, but if needed an alternative URL can be set which will instead attempt to download the file name from the root of the url

Still working out a few bugs, and making sure the logic handles so we don't try to upgrade twice, node properties seem to not be changable from the node, so it is a one way control plane

how does it work:
The agent dynamic service needs to be installed and then node properties from the controller ui will dictate how it acts
setting the following will cause it to perform the updates:

    "agent.upgrader.enabled" - boolean, setting this to true causes this service to come alive
    "agent.upgrader.version.max" - highest agent version to go to
    "agent.upgrader.version.min" - lowest agent version to maintain
    "agent.upgrader.version.preferred" - the version we want to be at, "22.8" will grab the latest agent of that major and minor version, so if a hotfix is published, it will be applied.
    "agent.upgrader.version.current" - this is not used on the controller side, and i'm a bit annoyed that it isn't propogated from agent to controller, it is what it is
    "agent.upgrader.repo.url" - alternative download site to get agent files from, se la vie

