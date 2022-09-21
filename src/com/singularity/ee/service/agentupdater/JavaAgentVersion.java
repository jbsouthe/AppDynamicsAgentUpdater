package com.singularity.ee.service.agentupdater;

//22.8.0.3333
public class JavaAgentVersion implements Comparable<JavaAgentVersion> {

    private String version;
    public int major=0, minor=-1, hotfix=-1, build=-1;
    public JavaAgentVersion( String versionString ) {
        if( versionString != null ) {
            this.version = versionString;
            String[] parts = versionString.split("\\.");
            if (parts.length > 0) major = Integer.parseInt(parts[0]);
            if (parts.length > 1) minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) hotfix = Integer.parseInt(parts[2]);
            if (parts.length > 3) build = Integer.parseInt(parts[3]);
        }
    }

    public String getVersion() { return version; }
    public String getDirectory() { return String.format("ver%s",this.version); }

    @Override
    public int compareTo(JavaAgentVersion other) {
        if( this.getVersion().equals(other.getVersion()) ) return 0; //if the two version strings match then skip to the end, otherwise start sorting...
        if (this.major < other.major) return -1;
        if (this.major > other.major) return 1;
        if ( other.minor == -1 ) return 0;
        if (this.minor < other.minor) return -1;
        if (this.minor > other.minor) return 1;
        if( other.hotfix == -1 ) return 0;
        if (this.hotfix < other.hotfix) return -1;
        if (this.hotfix > other.hotfix) return 1;
        if (other.build == -1 ) return 0;
        if (this.build < other.build) return -1;
        if (this.build > other.build) return 1;
        return 0; //else this must be equal, but the strings should have matched, oh well!
    }
}
