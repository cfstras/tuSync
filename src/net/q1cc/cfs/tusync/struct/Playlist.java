package net.q1cc.cfs.tusync.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.JCheckBox;
import net.q1cc.cfs.tusync.Main;

/**
 *
 * @author claus
 */
public class Playlist extends JCheckBox {
    public String title;
    public long id;
    public HashMap<Long,Title> tracks; //TODO heap? array?
    public boolean selected = false;
    
    public Playlist(String title, long id) {
        this.title=title;
        this.id = id;
        tracks = new HashMap<Long,Title>(64);
    }
    public synchronized void addTitle(Title t) {
        tracks.put(t.id,t);
    }
    @Override
    public String toString() {
        return title+" ("+tracks.size()+")";
    }
    
    @Override public void setSelected(boolean s) {
        super.setSelected(s);
        selected = s;
        Main.instance().tunesManager.reCheck();
    }
}
