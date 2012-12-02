/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    private Main main;
    public ArrayList<Playlist> playlists;
    public HashMap<Integer,Title> titles;
    private ReCheckThread reCheckThread;
    
    TunesModel libModel;
    public boolean recheck;
    long targetSize = 64*1000*1000*1000L;

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
        new Thread() {
            @Override
            public void run() {
                try {
                    Main.instance().tunesManager.doLoadLibrary();
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
                }
            }
        }.start();
    }

    public void doLoadLibrary() throws XmlParseException, IOException, TunesParseException {
        initLib();
        main.gui.progressBar.setString("Loading Library...");
        main.gui.progressBar.setIndeterminate(true);

        String path = main.props.getProperty("lib.basepath");
        if (path == null) {
            JOptionPane.showMessageDialog(main.gui, "Please select the path to your iTunes library first.");
            //TODO open dialog
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

        for (Map.Entry<String, Object> e : lib.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue().getClass());
        }

        loadTracks(lib);
        loadPlaylists(lib);
        main.gui.list.setModel(libModel);
        libModel.fireUpdate();
        //main.gui.tree.expandRow(1);
        //TODO serialize
        
        System.out.println("Fin.");
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
        int tracksPerProgress = numTracks/100;
        int num=0;
        int progress = 0;
        main.gui.progressBar.setMinimum(0);
        main.gui.progressBar.setMaximum(200);
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setString("reading tracks");
        main.gui.progressBar.setIndeterminate(false);
        
        Iterator<HashMap> it = tracks.values().iterator();
        while(it.hasNext()) {
            HashMap<String,Object> obj = it.next();
            it.remove();
            if(!obj.containsKey("Track ID")) {
                continue;
            }
            int id = (Integer) obj.get("Track ID");
            Title title = new Title(id);
            
            for(Entry<String,Object> e : obj.entrySet()) {
                int ind = Title.getAttInd(e.getKey());
                if(ind==-1) {
                    System.out.println("Error: no row named "+e.getKey());
                    continue;
                }
                Object val = e.getValue();
                if(val instanceof String) {
                    val = ((String)val).intern();
                }
                title.attribs[ind] = val;
            }
            titles.put(id, title);
            num++;
            if(num%tracksPerProgress==0 || num==numTracks) {
                main.gui.progressBar.setValue(++progress);
                main.gui.progressBar.setString("reading tracks: "+num+" / "+numTracks);
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
        int listsPerProgress = numLists/100;
        int num=0;
        int progress = 100;
        main.gui.progressBar.setMinimum(0);
        main.gui.progressBar.setMaximum(200);
        main.gui.progressBar.setValue(100);
        main.gui.progressBar.setString("reading playlists");
        main.gui.progressBar.setIndeterminate(false);
        
        Iterator<HashMap> it = lists.iterator();
        while(it.hasNext()) {
            HashMap list = it.next();
            it.remove();
            Playlist playlist = new Playlist(list.get("Name").toString(), (Integer)list.get("Playlist ID"));
            ArrayList<HashMap> entries = (ArrayList) list.get("Playlist Items");
            if(entries == null) {
                //no entries.
                continue;
            }
            Iterator<HashMap> listIt = entries.iterator();
            while(listIt.hasNext()) {
                HashMap<String,Integer> entry = listIt.next();
                playlist.addTitle(titles.get((Integer)entry.get("Track ID")));
            }
            playlists.add(playlist);
            System.out.println("Playlist "+playlist.title+": "+playlist.tracks.size()+" tracks.");
            num++;
            if (num % listsPerProgress == 0 || num == numLists) {
                main.gui.progressBar.setValue(++progress);
                main.gui.progressBar.setString("reading playlists: " + num + " / " + numLists);
            }
        }
        main.gui.progressBar.setValue(0);
        main.gui.progressBar.setString("");
        //playlists read. yay.
    }
    
    public long getPlaylistSize(Collection<Title> tracks, ArrayList<Long> ignore, long sizeBefore) {
        int attribID = Title.getAttInd("Size");
        long size = 0;
        for(Title t : tracks) {
            if(ignore.contains(t.id)) {
                continue;
            }
            t.selected = true;
            size += t.getSizeOnDisk();
            ignore.add(t.id);
            setProgressSize(targetSize, size+sizeBefore, false);
        }
        return size;
    }
    
    long lastSize = 0;
    public void reCheck() {
        recheck=true;
        reCheckThread.interrupt();
    }
    
    protected void doReCheck() {
        recheck=false;
        lastSize = 0;
        long size = 0;
        ArrayList<Long> ignore = new ArrayList<Long>(256);
        for(Playlist p : playlists) {
            if(p.selected) {
                size += getPlaylistSize(p.tracks.values(),ignore, size);
            }
        }
        setProgressSize(targetSize, size, true);
    }
    
    static final String progressIndicator = "|/-\\";
    private void setProgressSize(long targetSize, long size, boolean finished) {
        if(!finished && size-lastSize < 1024) {
            return;
        }
        long it = System.currentTimeMillis()/200;
        it %= progressIndicator.length();
        
        long sizeDiv = targetSize/500;
        main.gui.progressBar.setMaximum((int)(targetSize/sizeDiv));
        main.gui.progressBar.setValue((int)(size/sizeDiv));
        main.gui.progressBar.setString(humanize(size)+" / "+humanize(targetSize)+
                ((!finished)?" "+progressIndicator.charAt((int)it):" occupied."));
        main.gui.progressBar.setStringPainted(true);
        lastSize = size;
    }
    
    private static final String siPrefixes = " KMGTPE";
    private static String humanize(long bytes) {
        int prefixID = 0;
        double divided = bytes;
        while(divided >= 1000 && prefixID < siPrefixes.length()) {
            divided /= 1024;
            prefixID++;
        }
        return (Math.floor(divided*100)/100)+" "+siPrefixes.charAt(prefixID)+'B';
    }
    
    private class ReCheckThread extends Thread {
        boolean live = true;
        @Override
        public void run() {
            setName("ReCheckThread");
            while (live) {
                if(TunesManager.this.recheck) {
                    TunesManager.this.doReCheck();
                } else {
                    synchronized (this) {
                        try {
                            wait(5000);
                        } catch (InterruptedException ex) {}
                    }
                }
            }
        }
    }

    private class TunesModel implements TreeModel,ListModel {
        
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
            if(parent == root) {
                switch (index) {
                    case 0:
                        return "Playlists";
                    case 1:
                        return ""; //TODO tracks
                    default:
                        return null;
                }
            } else if(parent == playlistsNode) {
                return TunesManager.this.playlists.get(index);
            } else if(playlists.contains(parent)) {
                //return one title
                return null;
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if(parent == root) {
                return 1;
            } else if(parent == playlistsNode) {
                return TunesManager.this.playlists.size();
            } else if(TunesManager.this.playlists.contains(parent)){
                return 0;
                //return ((Playlist)parent).tracks.size();
            }
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            if(node == root || node == playlistsNode) {
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
            for(TreeModelListener l : treeeners) {
                l.treeStructureChanged(t);
            }
            ListDataEvent e = new ListDataEvent(TunesManager.this, ListDataEvent.INTERVAL_ADDED, 0, getSize());
            for(ListDataListener l : listeners) {
                l.contentsChanged(e);
            }
        }

        @Override
        public int getSize() {
            return TunesManager.this.playlists.size();
        }

        @Override
        public Object getElementAt(int index) {
            return  TunesManager.this.playlists.get(index);
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
}
