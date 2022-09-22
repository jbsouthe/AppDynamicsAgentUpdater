# AppDynamics agent upgrade dynamic service extension

Some setup. This should be installed in the <agent install dir>/ver22.###/external-services/agent-updater directory
the <agent intall dir>/ver22.###/conf/app-agent-config.xml has to have signing disabled:

    <configuration-properties>
        <property name="external-service-directory" value="external-services"/>
        <property name="enable-jar-signing" value="false"/>
    </configuration-properties>


The agent download from downloads.appdynamics.com requires authentication, this POC doesn't do all that, so the jar file has a friendly web server to host java agent zip files.

Still working out a few bugs, and making sure the logic handles so we don't try to upgrade twice, node properties seem to not be changable from the node, so it is a one way control plane

how does it work:
The agent dynamic service needs to be installed and then node properties from the controller ui will dictate how it acts
setting the following will cause it to perform the updates:

    "agent.upgrader.enabled" - boolean, setting this to true causes this service to come alive
    "agent.upgrader.version.max" - highest agent version to go to
    "agent.upgrader.version.min" - lowest agent version to maintain
    "agent.upgrader.version.preferred" - the version we want to be at
    "agent.upgrader.version.current" - this is not used on the controller side, and i'm a bit annoyed that it isn't propogated from agent to controller, it is what it is
    "agent.upgrader.repo.url" - alternative download site to get agent files from, se la vie

