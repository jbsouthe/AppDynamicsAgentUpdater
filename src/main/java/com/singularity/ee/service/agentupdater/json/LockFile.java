package com.singularity.ee.service.agentupdater.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;

import java.io.*;
import java.lang.management.ManagementFactory;

public class LockFile {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.json.LockFile");

    public String identifier;
    public transient String location;
    public long timestamp;
    public transient File file;
    private static Gson gson = new GsonBuilder().create();

    public LockFile() {}

    public static LockFile getLockFile( String fileName ) throws IOException {
        File lock = new File(fileName);
        LockFile lockFile = null;
        
        if( lock.exists() && lock.length()>0 ) { //read it
            lockFile = readLockFile(lock);
            if( lockFile == null ) return null;
            lockFile.location = lock.getAbsolutePath();
            lockFile.file = lock;
            return lockFile;
        } else { //make it
            lockFile = new LockFile();
            lockFile.identifier = ManagementFactory.getRuntimeMXBean().getName();
            lockFile.timestamp = System.currentTimeMillis();
            lockFile.file = lock;
            lockFile.write();
            try {
                Thread.sleep(2000); //network filesystems and blocking FS may not write immediately, so we should pause here
            } catch (InterruptedException e) {
                //ignore it
            }
            //now open it to make sure we return the lock that is made for the caller to decide next steps if it is not the one we think we just made
            LockFile actualLockFile = LockFile.readLockFile(lock);
            actualLockFile.location = lock.getAbsolutePath();
            actualLockFile.file = lock;
            return actualLockFile;
        }

    }

    private void write() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(this.file));
        gson.toJson(this, bw);
        bw.close();
    }

    private static LockFile readLockFile(File lock) throws IOException {
        LockFile lockFile;
        BufferedReader br = new BufferedReader(new FileReader(lock));
        try {
            lockFile = gson.fromJson(br, LockFile.class);
        } catch (JsonSyntaxException exception) {
            logger.error(String.format("Error trying to read the lock file %s, Exception: %s",lock.getAbsolutePath(),exception.toString()));
            return null;
        } finally {
            br.close();
        }
        if( lockFile == null ) {
            lockFile = new LockFile();
            lockFile.file=lock;
            lockFile.location=lock.getAbsolutePath();
            lockFile.identifier="EMPTY_IDENTIFIER";
            lockFile.timestamp=0;
        }
        return lockFile;
    }

    public String toString() {
        return String.format("file: %s age(ms): %d creator: %s", location, age(), identifier);
    }

    public boolean isThisMe() {
        return ManagementFactory.getRuntimeMXBean().getName().equals(identifier);
    }

    public long age() {
        return System.currentTimeMillis()-timestamp;
    }

    public void delete() {
        if( file != null ) file.delete();
    }
}
