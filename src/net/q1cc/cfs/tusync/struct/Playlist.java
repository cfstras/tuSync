/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author claus
 */
public class Playlist {
    public String title;
    public long id;
    public HashMap<Long,Title> tracks; //TODO heap? array?
    
    public Playlist(String title, long id) {
        this.title=title;
        this.id = id;
        tracks = new HashMap<Long,Title>(64);
    }
    public synchronized void addTitle(Title t) {
        tracks.put(t.id,t);
    }
}
