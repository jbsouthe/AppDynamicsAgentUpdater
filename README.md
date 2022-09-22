# AppDynamics agent upgrade dynamic service extension

Some setup. This should be installed in the <agent install dir>/ver22.###/external-services/agent-updater directory
the <agent intall dir>/ver22.###/conf/app-agent-config.xml has to have signing disabled:

    <configuration-properties>
        <property name="external-service-directory" value="external-services"/>
        <property name="enable-jar-signing" value="false"/>
    </configuration-properties>


The agent download from downloads.appdynamics.com requires authentication, this POC doesn't do all that, so the jar file has a friendly web server to host java agent zip files.

Still working out a few bugs, and making sure the logic handles so we don't try to upgrade twice, node properties seem to not be changable from the node, so it is a one way control plane

