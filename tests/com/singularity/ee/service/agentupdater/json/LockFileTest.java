package com.singularity.ee.service.agentupdater.json;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;

public class LockFileTest extends TestCase {

    private File existingFile, newFile;

    public void setUp() throws Exception {
        super.setUp();
        existingFile = new File("./tests/resources/otherHostLockFile.json");
    }

    @Test
    public void testLockFileCreateWhereExisting() throws Exception {
        assert existingFile.exists();
        LockFile lockFile = LockFile.getLockFile( existingFile.getPath());
        System.out.println("Existing Lockfile: "+ lockFile);
        assert lockFile.identifier.equals("12345@someOtherHost");
        assert !lockFile.isThisMe();
        assert lockFile.age()>(5*60000);
    }

    public void testLockFileCreateNew() throws Exception {
        File newFile = File.createTempFile("lockFile", ".json", new File("tests/resources") );
        newFile.delete(); //we just want the name
        LockFile lockFile = LockFile.getLockFile(newFile.getCanonicalPath());
        System.out.println("New Lockfile: "+ lockFile);
        assert lockFile.isThisMe();
        assert lockFile.age() < (5*60000);
        newFile.delete();
    }

    public void testLockFileEmptyFile() throws Exception {
        File emptyFile = File.createTempFile("lockFile", ".json", new File("tests/resources") );
        LockFile lockFile = LockFile.getLockFile(emptyFile.getCanonicalPath());
        System.out.println("New Lockfile: "+ lockFile);
        emptyFile.delete();
        assert lockFile.isThisMe();
        assert lockFile.age() < (5*60000);
    }

    public void testLockFileInvalidJSON() throws Exception {
        File badFile = new File("tests/resources/invalidLockFile.json");
        assert badFile.exists();
        LockFile lockFile = LockFile.getLockFile(badFile.getCanonicalPath());
        System.out.println("Bad Lockfile (should be null): "+ lockFile);
        assert lockFile == null;
    }

    public void testLockFileUseCaseLogic() throws Exception {
        File newFile = File.createTempFile("lockFile", ".json", new File("tests/resources") );
        newFile.delete(); //we just want the name
        LockFile lockFile = LockFile.getLockFile(newFile.getCanonicalPath());
        assert lockFile != null;
        if( lockFile != null && !lockFile.isThisMe() ) {
            boolean continueAnyway = false;
            if( lockFile.age() > 5*60000 ) { //if older than five minutes it is most likely stale
                continueAnyway = true;
            }
            System.out.println(String.format("Agent Updater attempted to get lock file, found an existing one '%s' will continue? %s", lockFile, continueAnyway));
            if( continueAnyway ) {
                lockFile.delete();
                lockFile = LockFile.getLockFile(newFile.getCanonicalPath());
                if ( lockFile == null || !lockFile.isThisMe()) {
                    continueAnyway = false;
                    System.out.println(String.format("Agent Updater attempted to get lock file a second time and still found an existing one '%s' will continue? %s", lockFile, continueAnyway));
                }
                lockFile = null;
            } else {
                lockFile = null;
            }
        }
        assert lockFile != null;
        lockFile.delete();
    }

    public void tearDown() throws Exception {
        //if( newFile != null && newFile.exists() ) newFile.delete();
    }
}