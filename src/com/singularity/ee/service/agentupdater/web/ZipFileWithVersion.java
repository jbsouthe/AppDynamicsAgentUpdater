package com.singularity.ee.service.agentupdater.web;

import com.singularity.ee.service.agentupdater.JavaAgentVersion;

import java.io.File;

public class ZipFileWithVersion {
    public File zipFile;
    public JavaAgentVersion javaAgentVersion;

    public ZipFileWithVersion(File zipFile, String version) {
        this.zipFile=zipFile;
        this.javaAgentVersion = new JavaAgentVersion(version);
    }
}
