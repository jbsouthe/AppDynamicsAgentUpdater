package com.singularity.ee.service.agentupdater.json;

import java.io.File;
import java.io.IOException;

public class DownloadDetails {

    public DownloadDetails() {} //for GSON
    public DownloadDetails( int id, File file ) throws IOException {
        this.id=id;
        this.filename=file.getName();
        this.filetype="java-jdk8";
        this.s3_path=filename;
        this.javaAgentVersion = JavaAgentVersion.getJavaAgentVersion(file);
        this.version = this.javaAgentVersion.getVersion();
    }
    public int id;
    public String version, download_path, filename, title, filetype, os, s3_path;
    private JavaAgentVersion javaAgentVersion;
    public boolean matches( JavaAgentVersion otherJavaAgentVersion ) {
        if( javaAgentVersion.compareTo( otherJavaAgentVersion ) == 0 ) return true;
        return false;
    }
}
