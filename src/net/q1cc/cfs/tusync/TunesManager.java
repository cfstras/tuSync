package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import net.q1cc.cfs.tusync.struct.*;
import xmlwise.Plist;
import xmlwise.XmlParseException;

/**
 *
 * @author cfstras
 */
public class TunesManager {
    
    public final static String[] tunesTitleFolders = {
    "Audiobooks", "Automatically Add to iTunes", "Automatisch zu iTunes hinzufügen",
    "iPod Games", "iTunes U", "Mobile Applications", "Movies", "Music", "Podcasts",
    "Ringtones", "Tones", "TV Shows"};
    
    private Main main;
    public ArrayList<Playlist> playlists;
    public HashMap<Integer, Title> titles;
    private ReCheckThread reCheckThread;
    private HashSet<Title> titlesToSync;
    public String baseFolder;
    TunesModel libModel;
    public boolean recheck;
    boolean checkingSize;
    boolean loadingLib;
    boolean syncingLib;
    long targetSize = 64 * 1000 * 1000 * 1000L;

    public TunesManager() {
        main = Main.instance();
        initLib();
        libModel = new TunesModel();
        reCheckThread = new ReCheckThread();
        reCheckThread.setPriority(Thread.MIN_PRIORITY);
        reCheckThread.start();
    }

    private void initLib() {
        playlists = new ArrayList<Playlist>(32);
        titles = new HashMap<Integer, Title>(256);
    }

    public void loadLibrary() {
        Thread libt = new Thread() {
            @Override
            public void run() {
                try {
                    loadingLib = true;
                    main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
                    doLoadLibrary();
                } catch (TunesParseException e) {
                    System.out.println("Parse Error: " + e);
                } catch (XmlParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    main.gui.progressBar.setIndeterminate(false);
                    main.gui.progressBar.setString("");
                    main.gui.progressBar.setValue(0);
                    loadingLib = false;
                    main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
                }
            }
        };
        libt.setName("LoadLibThread");
        libt.setPriority((Thread.MIN_PRIORITY+Thread.MAX_PRIORITY)/2);
        libt.start();
    }

    public void syncLibrary() {
        Thread synt = new Thread() {
            @Override
            public void run() {
                syncingLib=true;
                main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
                main.gui.setListEnabled(false);
                doSyncLibrary();
                syncingLib=false;
                main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
                main.gui.setListEnabled(true);
            }
        };
        synt.setName("SyncThread");
        synt.setPriority((Thread.MIN_PRIORITY+Thread.MAX_PRIORITY)/2);
        synt.start();
    }

    private void doLoadLibrary() throws XmlParseException, IOException, TunesParseException {
        initLib();
        main.gui.progressBar.setString("Loading Library...");
        main.gui.progressBar.setIndeterminate(true);

        String path = main.props.get("lib.basepath", null);
        if (path == null) {
            JOptionPane.showMessageDialog(main.gui, "Please select the path to your iTunes library first.");
            return;
        }
        File tunesFolder = new File(path);
        File xmlFile = new File(tunesFolder.getAbsolutePath() + System.getProperty("file.separator") + "iTunes Music Library.xml");
        if (!xmlFile.exists()) {
            JOptionPane.showMessageDialog(main.gui, "Sorry, but i couldn't find your Library XML file.");
            //TODO find reason why xml was not found
            return;
        }
        Map<String, Object> lib = Plist.load(xmlFile);

        //for (Map.Entry<String, Object> e : lib.entrySet()) {
        //    System.out.println(e.getKey() + ": " + e.getValue().getClass());
        //}

        baseFolder = new File(Title.decodeLocation(lib.get("Music Folder").toString())).getAbsolutePath().concat(File.separator);
        Title.baseFolder = baseFolder;
        
        loadTracks(lib);
        loadPlaylists(lib);
        main.gui.list.setModel(libModel);
        libModel.fireUpdate();
        
        System.gc();
        //TODO serialize
    }

    void loadTracks(Map<String, Object> lib) throws TunesParseException {
        Object tr = lib.remove("Tracks");
        HashMap<String, HashMap> tracks = null;
        if (tr instanceof HashMap) {
            tracks = (HashMap<String, HashMap>) tr;
        }

        if (tracks == null) {
            throw new TunesParseException("no tracks");
        }

        int numTracks = tracks.size();
        int tracksPerProgress = numTracks / 100;
        int num = 0;
        int progress = 0;
        main.gui.progressBar.setMinimum(0);
        main.gui.progressBar.setMaximum(200);
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setString("reading tracks");
        main.gui.progressBar.setStringPainted(true);
        main.gui.progressBar.setIndeterminate(false);

        Iterator<HashMap> it = tracks.values().iterator();
        while (it.hasNext()) {
            HashMap<String, Object> obj = it.next();
            it.remove();
            if (!obj.containsKey("Track ID")) {
                continue;
            }
            int id = (Integer) obj.get("Track ID");
            Title title = new Title(id);

            for (Entry<String, Object> e : obj.entrySet()) {
                int ind = Title.getAttInd(e.getKey());
                if (ind == -1) {
                    System.out.println("Error: no row named " + e.getKey());
                    continue;
                }
                Object val = e.getValue();
                if (val instanceof String) {
                    val = ((String) val).intern();
                }
                title.attribs[ind] = val;
            }
            titles.put(id, title);
            num++;
            if (num % tracksPerProgress == 0 || num == numTracks) {
                main.gui.progressBar.setValue(++progress);
                main.gui.progressBar.setString("reading tracks: " + num + " / " + numTracks);
            }

        }
        //tracks loaded.

    }

    void loadPlaylists(Map<String, Object> lib) throws TunesParseException {
        //playlists!
        Object pl = lib.remove("Playlists");
        ArrayList<HashMap> lists = null;
        if (pl instanceof ArrayList) {
            lists = (ArrayList<HashMap>) pl;
        }

        if (lists == null) {
            throw new TunesParseException("no playlists");
        }

        int numLists = lists.size();
        int listsPerProgress = numLists / 100;
        int num = 0;
        int progress = 100;
        main.gui.progressBar.setMinimum(0);
        main.gui.progressBar.setMaximum(200);
        main.gui.progressBar.setValue(100);
        main.gui.progressBar.setString("reading playlists");
        main.gui.progressBar.setStringPainted(true);
        main.gui.progressBar.setIndeterminate(false);

        Iterator<HashMap> it = lists.iterator();
        while (it.hasNext()) {
            HashMap list = it.next();
            it.remove();
            Playlist playlist = new Playlist(list.get("Name").toString(), (Integer) list.get("Playlist ID"));
            playlist.persID = list.get("Playlist Persistent ID").toString();
            Object ppid = list.get("Parent Persistent ID");
            if(ppid!=null) {
                playlist.parentPersID = ppid.toString();
            }
            ArrayList<HashMap> entries = (ArrayList) list.get("Playlist Items");
            if (entries == null) {
                //no entries.
                continue;
            }
            Iterator<HashMap> listIt = entries.iterator();
            while (listIt.hasNext()) {
                HashMap<String, Integer> entry = listIt.next();
                playlist.addTitle(titles.get((Integer)entry.get("Track ID")));
            }
            playlists.add(playlist);
            //System.out.println("Playlist "+playlist.title+": "+playlist.tracks.size()+" tracks.");
            num++;
            if (num % listsPerProgress == 0 || num == numLists) {
                main.gui.progressBar.setValue(++progress);
                main.gui.progressBar.setString("reading playlists: " + num + " / " + numLists);
            }
        }
        //TODO sort playlists
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setString("");
        //playlists read. yay.
    }

    private void doSyncLibrary() {
        syncingLib=true;
        main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
        String targetPath = main.props.get("lib.targetpath", null);
        if(targetPath == null) {
            JOptionPane.showMessageDialog(main.gui, "No target path selected! Please select one.", "No target!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File targetPathFile = new File(targetPath);
        targetPathFile.mkdirs();
        if(!targetPathFile.canRead()) {
            JOptionPane.showMessageDialog(main.gui, "Can't read target path", "Read error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(!targetPathFile.canWrite()) {
            JOptionPane.showMessageDialog(main.gui, "Can't write target path", "Write error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        syncPlaylists(targetPathFile);
        syncTitles(targetPathFile);
        titlesToSync = null;
        
        syncingLib=false;
        main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
        
        System.gc();
    }
    
    private void syncPlaylists(File targetPathFile) {
        main.gui.progressBar.setString("writing playlists...");
        main.gui.progressBar.setStringPainted(true);
        main.gui.progressBar.setMaximum(110);
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setIndeterminate(false);
        int playlistsPerStep = Math.max(1,playlists.size()/10);
        int i=0, value=0;
        Iterator<Playlist> plIt = playlists.iterator();
        
        if(main.props.getBoolean("sync.deleteotherplaylists", false)) {
            main.gui.progressBar.setIndeterminate(true);
            main.gui.progressBar.setString("deleting old playlists...");
            for( File f: targetPathFile.listFiles()) {
                if(f.getName().endsWith(".m3u")) {
                    f.delete();
                }
            }
            main.gui.progressBar.setString("writing playlists...");
            main.gui.progressBar.setIndeterminate(false);
        }
        
        titlesToSync = new HashSet<Title>(256);
        while(plIt.hasNext()) {
            PrintWriter out = null;
            try {
                if(i++ % playlistsPerStep == 0) {
                    main.gui.progressBar.setValue(++value);
                }
                Playlist playlist = plIt.next();
                if(!playlist.selected) {
                    continue;
                }
                String filename = playlist.title.replaceAll("[^\\w \\-]", "-").concat(".m3u");
                File playlistFile = new File(targetPathFile.getAbsolutePath()+File.separator+filename);
                System.out.println("Playlist "+playlist.title+" --> "+filename);
                playlistFile.createNewFile();
                out = new PrintWriter(new FileWriter(playlistFile, false));
                
                out.println("#EXTM3U");
                Iterator<Title> titleIt = playlist.tracks.values().iterator();
                while(titleIt.hasNext()) {
                    Title t = titleIt.next();
                    titlesToSync.add(t);
                    out.print("#EXTINF:");
                    out.print(t.getLength()/100);
                    out.print(", ");
                    out.print(t.attribs[Title.getAttInd("Artist")]);
                    out.print(" - ");
                    out.println(t.attribs[Title.getAttInd("Name")]);
                    out.println(Title.getPathRelative(new File(t.file.toString()).getAbsolutePath()));
                }
                
                out.println();
                out.flush();
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if(out!=null) {
                    out.flush();
                    out.close();
                }
            }
            main.gui.progressBar.setValue(10);
            
        }
        
    }
    
    private void syncTitles(File targetPathFile) {
        main.gui.progressBar.setString("syncing titles...");
        
        int titlesPerValue = Math.max(1,titlesToSync.size() / 100);
        int value = 10, i = 0, iMax = titlesToSync.size();
        
        HashSet<Path> filesInTarget = new HashSet<Path>(256);
        if(main.props.getBoolean("sync.deleteothertitles", false)) {
            main.gui.progressBar.setIndeterminate(true);
            main.gui.progressBar.setString("building file list...");
            try {
                Files.walkFileTree(targetPathFile.toPath(), new FileLister(filesInTarget));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            main.gui.progressBar.setString("syncing titles...");
            main.gui.progressBar.setIndeterminate(false);
        }
        
        Iterator<Title> titleIt = titlesToSync.iterator();
        while(titleIt.hasNext()) {
            if(i++ % titlesPerValue == 0) {
                main.gui.progressBar.setString("syncing titles: "+i+" / "+iMax);
                main.gui.progressBar.setValue(10 + ++value);
            }
            Title t = titleIt.next();
            String relpath = Title.getPathRelative(new File(t.file.toString()).getAbsolutePath());
            File targetfile = new File(targetPathFile.getAbsolutePath()+File.separator+relpath);
            if(t.file == null) {
                t.getSizeOnDisk();
            }
            if(t.file == null) {
                System.out.println("File not found: "+t.toString());
                continue;
            }
            try {
                filesInTarget.remove(targetfile.toPath());
                Files.createDirectories(targetfile.toPath());
                //targetfile.getParentFile().mkdirs();
                copyFile(t.file, targetfile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        main.gui.progressBar.setValue(110);
        main.gui.progressBar.setString("Titles successfully synced!");
        
        if(main.props.getBoolean("sync.deleteothertitles", false)) {
            main.gui.progressBar.setString("Deleting other files...");
            main.gui.progressBar.setIndeterminate(true);
            System.out.println("other files deleted:");
            for(Path t:filesInTarget) { // delete any files remaining in dir
                System.out.println(t); //TODO delete beforehand, in case we're running low on space
                try {
                    Files.delete(t);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            main.gui.progressBar.setIndeterminate(false);
            main.gui.progressBar.setString("Titles successfully synced!");
        }
        System.out.println("meh.");
    }
    
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        } else {
            if(sourceFile.length() == destFile.length()
            && sourceFile.lastModified() <= destFile.lastModified()) {
                DateFormat t = DateFormat.getDateTimeInstance();
                System.out.println("Skipping: srcTime:"+t.format(new Date(sourceFile.lastModified()))
                +" dstTime: "+t.format(new Date(destFile.lastModified()))+ " size:"+sourceFile.length()
                +" - "+destFile.getAbsolutePath());
                return;
            }
        }

        Files.copy(sourceFile.toPath(),destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    long lastSize = 0;

    public long getPlaylistSize(Collection<Title> tracks, HashSet<Title> ignore, long sizeBefore) {
        int attribID = Title.getAttInd("Size");
        long size = 0;
        for (Title t : tracks) {
            if (ignore.contains(t)) {
                continue;
            }
            t.selected = true;
            size += t.getSizeOnDisk();
            ignore.add(t);
            setProgressSize(targetSize, size + sizeBefore, false);
        }
        return size;
    }
    
    public void reCheck() {
        recheck = true;
        main.gui.setSyncButton(true, syncingLib, loadingLib);
        reCheckThread.interrupt();
    }

    protected void doReCheck() {
        recheck = false;
        checkingSize = true;
        main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
        lastSize = 0;
        long size = 0;
        HashSet<Title> ignore = new HashSet<Title>(256);
        for (Playlist p : playlists) {
            if (p.selected) {
                size += getPlaylistSize(p.tracks.values(), ignore, size);
            }
        }
        setProgressSize(targetSize, size, true);
        checkingSize = false;
        main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
        System.gc();
    }
    static final String progressIndicator = "|/-\\";

    private void setProgressSize(long targetSize, long size, boolean finished) {
        if (!finished && size - lastSize < 1024) {
            return;
        }
        long it = System.currentTimeMillis() / 200;
        it %= progressIndicator.length();

        long sizeDiv = targetSize / 500;
        main.gui.progressBar.setMaximum((int) (targetSize / sizeDiv));
        main.gui.progressBar.setValue((int) (size / sizeDiv));
        main.gui.progressBar.setString(humanize(size) + " / " + humanize(targetSize)
                + ((!finished) ? " " + progressIndicator.charAt((int) it) : " occupied."));
        main.gui.progressBar.setStringPainted(true);
        lastSize = size;
    }
    private static final String siPrefixes = " KMGTPE";

    private static String humanize(long bytes) {
        int prefixID = 0;
        double divided = bytes;
        while (divided >= 1000 && prefixID < siPrefixes.length()) {
            divided /= 1024;
            prefixID++;
        }
        return (Math.floor(divided * 100) / 100) + " " + siPrefixes.charAt(prefixID) + 'B';
    }

    void toggleSelected(Playlist playlist) {
        if (syncingLib) {
            return;
        }
        playlist.setSelected(!playlist.selected);
        Main.instance().tunesManager.reCheck();
    }

    private class ReCheckThread extends Thread {

        boolean live = true;

        @Override
        public void run() {
            setName("ReCheckThread");
            while (live) {
                if (TunesManager.this.recheck) {
                    TunesManager.this.doReCheck();
                } else {
                    synchronized (this) {
                        try {
                            wait(5000);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }

    private class TunesModel implements TreeModel, ListModel {

        public String root = "Music";
        String playlistsNode = "Playlists";
        public ArrayList<TreeModelListener> treeeners = new ArrayList<TreeModelListener>(2);
        public ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>(2);

        public TunesModel() {
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent == root) {
                switch (index) {
                    case 0:
                        return "Playlists";
                    case 1:
                        return ""; //TODO tracks
                    default:
                        return null;
                }
            } else if (parent == playlistsNode) {
                return TunesManager.this.playlists.get(index);
            } else if (playlists.contains(parent)) {
                //return one title
                return null;
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent == root) {
                return 1;
            } else if (parent == playlistsNode) {
                return TunesManager.this.playlists.size();
            } else if (TunesManager.this.playlists.contains(parent)) {
                return 0;
                //return ((Playlist)parent).tracks.size();
            }
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            if (node == root || node == playlistsNode) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            //TODO
            return 0;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            treeeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            treeeners.remove(l);
        }

        public void fireUpdate() {
            TreeModelEvent t = new TreeModelEvent(root, new TreePath(root));
            for (TreeModelListener l : treeeners) {
                l.treeStructureChanged(t);
            }
            ListDataEvent e = new ListDataEvent(TunesManager.this, ListDataEvent.INTERVAL_ADDED, 0, getSize());
            for (ListDataListener l : listeners) {
                l.contentsChanged(e);
            }
        }

        @Override
        public int getSize() {
            return TunesManager.this.playlists.size();
        }

        @Override
        public Object getElementAt(int index) {
            return TunesManager.this.playlists.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }
    }

    private static class FileDeleter implements FileVisitor {

        public FileDeleter() {
        }

        @Override
        public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
            Files.delete((Path) file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
            Files.delete((Path) dir);
            return FileVisitResult.CONTINUE;
        }
    }
    
    private static class FileLister implements FileVisitor {
        HashSet<Path> files;
        boolean dirEmpty = false;
        public FileLister(HashSet<Path> files) {
            this.files = files;
        }

        @Override
        public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
            dirEmpty = true;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
            if(attrs.isRegularFile()){
                dirEmpty = false;
                files.add((Path)file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
            if(dirEmpty) { // delete directory if empty
                boolean empty = true;
                Path p = (Path)dir;
                Iterator<Path> it = p.iterator();
                while(it.hasNext()) { // double-check whether empty
                    if(!it.next().toFile().isDirectory()) {
                        empty = false;
                        break;
                    }
                }
                if(empty) {
                    Files.delete(p);
                    return FileVisitResult.CONTINUE;
                }
            }
            files.add((Path)dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
