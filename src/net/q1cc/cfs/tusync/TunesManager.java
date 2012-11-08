/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.util.ArrayList;
import javax.swing.JOptionPane;
import net.q1cc.cfs.tusync.struct.*;

/**
 *
 * @author claus
 */
public class TunesManager {
    
    private Main main;
    public ArrayList<Playlist> playlists;
    
    public TunesManager() {
        main = Main.instance();
        playlists = new ArrayList<Playlist>(32);
    }
    
    public void loadLibrary() {
        String path = main.props.getProperty("lib.basepath");
        if(path==null) {
            JOptionPane.showMessageDialog(main.gui,"Please select the path to your iTunes library first.");
            //TODO open dialog
            return;
        }
        
        
    }
    
}
