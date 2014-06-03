package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.q1cc.cfs.tusync.struct.Playlist;
import net.q1cc.cfs.tusync.struct.Title;
import xmlwise.Plist;
import xmlwise.XmlParseException;

/**
 *
 * @author cfstras
 */
public class TunesManager {
    
    private final Main main;
    public ArrayList<Playlist> playlists;
    public HashMap<Integer, Title> titles;
    private final ReCheckThread reCheckThread;
    private SyncTarget target;
    public Path baseFolder;
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

        if(Main.instance().props.getBoolean("lib.lastLoadWasSuccessful", false)) {
            loadLibrary();
        }
    }

    private void initLib() {
        playlists = new ArrayList<>(32);
        titles = new HashMap<>(256);
    }

    public final void loadLibrary() {
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
                    e.printStackTrace();
                } catch (IOException e) {
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

        main.props.putBoolean("lib.lastLoadWasSuccessful",false);

        String path = main.props.get("lib.xmlfile", null);
        if (path == null) {
            JOptionPane.showMessageDialog(main.gui, "Please select the path to your iTunes library first.");
            return;
        }
        File xmlFile = new File(path);
        if (!xmlFile.exists()) {
            JOptionPane.showMessageDialog(main.gui, "Sorry, but i couldn't find your Library XML file.");
            //TODO find reason why xml was not found
            return;
        }
        Map<String, Object> lib = Plist.load(xmlFile);

        //for (Map.Entry<String, Object> e : lib.entrySet()) {
        //    System.out.println(e.getKey() + ": " + e.getValue().getClass());
        //}

        baseFolder = new File(Title.decodeLocation(lib.get("Music Folder").toString()))
            .getAbsoluteFile().toPath().normalize();
        Title.baseFolder = baseFolder;
        
        loadTracks(lib);
        loadPlaylists(lib);
        lib = null;
        main.gui.list.setModel(libModel);
        libModel.fireUpdate();
        if(main.props.getBoolean("lib.lastLoadWasSuccessful",false)) {
            //select last selected playlists
            main.gui.selectLastButton.setSelected(true);
            main.gui.repaint();
            loadSelectedPlaylists();
            main.gui.repaint();
        }
        
        main.props.putBoolean("lib.lastLoadWasSuccessful",true);
        postLibLoadHook();
        System.gc();
        //TODO we could serialize our database here.
    }
    
    @SuppressWarnings("unchecked")
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
                    //error was already printed, ignore attribute
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

    @SuppressWarnings("unchecked")
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
        int listsPerProgress = Math.max(1,numLists/100);
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
                playlist.addTitle(titles.get(entry.get("Track ID")));
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

        target = new FileTarget(targetPathFile.toPath());
        syncPlaylists();
        syncTitles();
        target = null;
        
        syncingLib=false;
        main.gui.setSyncButton(checkingSize, syncingLib, loadingLib);
        
        System.gc();
    }
    
    private void syncPlaylists() {
        main.gui.progressBar.setString("writing playlists...");
        main.gui.progressBar.setStringPainted(true);
        main.gui.progressBar.setMaximum(1100);
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setIndeterminate(false);
        int numPlaylists = playlists.size();
        int percent = 0, i = 0;
        Iterator<Playlist> plIt = playlists.iterator();
        
        if(main.props.getBoolean("sync.deleteotherplaylists", false)) {
            main.gui.progressBar.setIndeterminate(true);
            main.gui.progressBar.setString("deleting old playlists...");
            target.deleteOldPlaylists();
            main.gui.progressBar.setString("writing playlists...");
            main.gui.progressBar.setIndeterminate(false);
        }
        
        while(plIt.hasNext()) {
            Playlist list = plIt.next();
            i++;
            int npercent = i * 100 / numPlaylists;
            if(npercent > percent) {
                percent = npercent;
                main.gui.progressBar.setValue(percent);
            }
            if (!list.selected) {
                continue;
            }
            ArrayList<SyncElement> elements = list.write(target);
            target.addFiles(elements);
        }
        main.gui.progressBar.setValue(100);
    }
    
    private void syncTitles() {
        target.startSync();
        StateThread status = new StateThread(target);
        status.start();
        target.waitFinish();
        status.run = false;
        try {
            status.join();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        System.out.println("success.");
    }

    private class StateThread extends Thread{
        boolean run = true;
        SyncTarget target;
        public StateThread(SyncTarget target) {
            this.target = target;
        }
        @Override public void run() {
            while (run) {
                Main.instance().gui.progressBar.setValue(100+(int)(target.getProgress()*1000));
                Main.instance().gui.progressBar.setString(target.getState());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            }
            main.gui.progressBar.setValue(1100);
            main.gui.progressBar.setString("Titles successfully synced!");
        }
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
        HashSet<Title> ignore = new HashSet<>(256);
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
        main.gui.progressBar.setString(Util.humanize(size) + " / " + Util.humanize(targetSize)
                + ((!finished) ? " " + progressIndicator.charAt((int) it) : " occupied."));
        main.gui.progressBar.setStringPainted(true);
        lastSize = size;
    }

    void toggleSelected(Playlist playlist) {
        if (syncingLib) {
            return;
        }
        playlist.setSelected(!playlist.selected);
        reCheck();
    }

    void saveSelectedPlaylists() {
        Preferences p = main.props;
        StringBuilder sb = new StringBuilder();
        for(Playlist pl : playlists) {
            if(pl.selected) {
                sb.append(pl.persID);
                sb.append(';');
            }
        }
        p.put("playlists.selected", sb.toString());
    }

    void loadSelectedPlaylists() {
        Preferences p = main.props;
        String sel = p.get("playlists.selected", "");
        if(sel==null && sel.equals("")) {
            System.out.println("no selected playlists saved");
            return;
        }
        for(Playlist pl : playlists) {
            pl.selected=false;
        }
        StringTokenizer st = new StringTokenizer(sel,";");
        while(st.hasMoreElements()) {
            String persID = st.nextToken();
            for(Playlist pl : playlists) {
                if(pl.persID != null && pl.persID.equals(persID)) {
                    pl.selected = true;
                    pl.setSelected(true);
                }
            }
        }
        main.gui.list.repaint();
        reCheck();
    }

    private void postLibLoadHook() {
        //debug
        HashSet<String> s = new HashSet<>();
        for(Title t:titles.values()) {
            String type = (String)t.attribs[Title.getAttInd("Kind")];
            if (!s.contains(type)) {
                s.add(type);
                System.out.println(type);
            }
        }
        
        System.out.println("   lol");
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

    private class TunesModel implements TreeModel, ListModel<Object> {

        public String root = "Music";
        String playlistsNode = "Playlists";
        public ArrayList<TreeModelListener> treeeners = new ArrayList<>(2);
        public ArrayList<ListDataListener> listeners = new ArrayList<>(2);

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
                        return playlistsNode;
                    case 1:
                        return ""; //TODO artists/genres/collections
                    default:
                        return null;
                }
            } else if (parent == playlistsNode) {
                return TunesManager.this.playlists.get(index);
            } else if (parent instanceof Playlist
                && playlists.contains(parent)) {
                //TODO expand playlist to titles
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
            } else if (parent instanceof Playlist
                && TunesManager.this.playlists.contains(parent)) {
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
            if(child == root) {
                return 0;
            } else if(child == playlists) {
                return 0;
            } else if (parent == playlists){
                TunesManager.this.playlists.indexOf(child);
            }
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

    private static class FileDeleter implements FileVisitor<Path> {
        HashMap<Path, Title> notThese;
        Path basePath;
        boolean empty = false;

        public FileDeleter(HashMap<Path, Title> notThese, Path basePath) {
            this.notThese = notThese;
            this.basePath = basePath;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            empty = true;
            if(attrs.isSymbolicLink()) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relpath = basePath.relativize(file);
            Title t = notThese.remove(relpath);
            if(t == null) { // The file is not in our list of songs to sync, delete it
                Files.delete(file);
                System.out.println("deleting "+file);
            } else {
                empty = false;
                Path source = t.getFile();
                if(Files.size(source) == Files.size(file)
                && Math.abs(Files.getLastModifiedTime(source).toMillis() - Files.getLastModifiedTime(file).toMillis())
                    <= TimeUnit.DAYS.toMillis(2))  {
                    //skip if source older than destination, 2 days ignored
                    /*System.out.println("Skipping: srcTime:"+(Files.getLastModifiedTime(source).toString())
                    +" dstTime: "+Files.getLastModifiedTime((Path)file).toString()+ " size:"+Files.size(source)
                    +" src: "+source+" dest: "+(Path)file);*/
                } else {
                    //put it back in
                    System.out.println("replacing: srcTime:"+(Files.getLastModifiedTime(source).toString())
                    +" dstTime: "+Files.getLastModifiedTime(file).toString()+ " size:"+Files.size(source)
                    +" src: "+source+" dest: "+file);
                    notThese.put(relpath, t);
                }
                
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if(empty) {
                boolean reallyEmpty = true;
                Iterator<Path> it = dir.iterator();
                while(it.hasNext()) { // double-check whether empty
                    //TODO recursively delete subdirs
                    if(! Files.isDirectory(it.next())) {
                        reallyEmpty = false;
                        break;
                    }
                }
                if(reallyEmpty) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            }
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
                    empty = false;
                    break;
                }
                if(empty) {
                    Files.delete(p);
                    return FileVisitResult.CONTINUE;
                } else {
                    files.add((Path)dir);
                }
            } else {
                files.add((Path)dir);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
