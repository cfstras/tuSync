package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class FileTarget implements SyncTarget {

    private final Main main;
    private final ArrayList<SyncElement> files;
    private final Path basePath;

    private final String state = null;
    private Thread syncer = null;
    private float progress = 0;
    
    public FileTarget(Path basePath) {
        main = Main.instance();
        this.basePath = basePath;
        files = new ArrayList<SyncElement>();
    }

    @Override
    public void addFiles(Collection<SyncElement> files) {
        this.files.addAll(files);
    }

    @Override
    public void startSync() {
        if (syncer != null) {
            return;
        }
        syncer = new Thread() {
            @Override public void run() {
                doSync();
            }
        };
        syncer.start();
    }
    
    private void doSync() {
        progress = 0;
        if(main.props.getBoolean("sync.deleteothertitles", false)) {
            //walk dir and delete any extra files.
            state = "deleting other files...";
            System.out.println("deleting other titles and building filelist");
            try {
                for (String s : Util.tunesTitleFolders){
                    Files.walkFileTree(new File(basePath+File.separator+s).toPath(),
                        new FileDeleter(files, basePath.toFile().getAbsolutePath()));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            state = "syncing titles...";
        }
        long totalBytes = 0, copiedBytes = 0, i = 0,
                iMax = files.size();
        for(SyncElement t : files) {
            totalBytes += t.size;
        }

        Iterator<SyncElement> titleIt = files.iterator();
        while(titleIt.hasNext()) {
            SyncElement t = null;
            main.gui.progressBar.setString("syncing titles: "+i+" / "+iMax+", "+
                    Util.humanize(copiedBytes)+" / "+Util.humanize(totalBytes));
            t = titleIt.next();
            Path targetfile = new File(basePath +File.separator+ t.destPath).toPath();
            boolean success = false;
            for(int tries = 0; tries < 3 && !success; tries++) {
                if(tries>0) {
                    System.out.println("trying again...");
                }
                try {
                    Path source = t.title.getFile();
                    if(Files.exists(targetfile, LinkOption.NOFOLLOW_LINKS)) {
                        try {
                            if(Files.size(source) == Files.size(targetfile)
                            && Math.abs(Files.getLastModifiedTime(source).toMillis() - Files.getLastModifiedTime(targetfile).toMillis())
                                <= TimeUnit.DAYS.toMillis(1)) {
                                // skip if source older than destination, 1 day ignored
                                break;
                            } else {
                                // put it back in
                                System.out.println("replacing: srcTime:"+(Files.getLastModifiedTime(source).toString())
                                +" dstTime: "+Files.getLastModifiedTime(targetfile).toString()+ " size:"+Files.size(source)
                                +" src: "+source+" dest: "+targetfile);
                            }
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    }
                    
                    Files.createDirectories(targetfile.getParent());
                    System.out.println("copying "+source +" to "+ targetfile);
                    Files.copy(source, targetfile, StandardCopyOption.REPLACE_EXISTING);
                    copiedBytes += t.size;
                    progress = copiedBytes*1.0f / totalBytes;
                    i++;
                    break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InvalidPathException ex) {
                    if(t!=null){
                        System.out.println("Error at "+t.toString()+": "+ex);
                    } else {
                        System.out.println("Error at "+ex);
                    }
                }
            }
        }

    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public void waitFinish() {
        if (syncer != null) {
            while (syncer != null && syncer.isAlive()) {
            try {
                syncer.join();
            } catch (InterruptedException e) {}
            }
        }
        syncer = null;
    }

    @Override
    public void deleteOldPlaylists() {
        for(File f: basePath.toFile().listFiles()) {
            if(f.getName().endsWith(".m3u")) {
                f.delete();
            }
        }
    }

    @Override
    public String getState() {
        return state;
    }

}
