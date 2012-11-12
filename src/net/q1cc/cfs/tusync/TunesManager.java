/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JOptionPane;
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
    
    public TunesManager() {
        main = Main.instance();
        playlists = new ArrayList<Playlist>(32);
        
    }
    
    public void loadLibrary() {
    	new Thread(){
			@Override public void run() {
				try {
					Main.instance().tunesManager.loadLibraryE();
				} catch (XmlParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
    }
    
    public void loadLibraryE() throws XmlParseException, IOException {
        String path = main.props.getProperty("lib.basepath");
        if(path==null) {
            JOptionPane.showMessageDialog(main.gui.frame,"Please select the path to your iTunes library first.");
            //TODO open dialog
            return;
        }
        File tunesFolder = new File(path);
        File xmlFile = new File(tunesFolder.getAbsolutePath()+System.getProperty("file.separator")+"iTunes Music Library.xml");
        if(!xmlFile.exists()) {
        	JOptionPane.showMessageDialog(main.gui.frame,"Sorry, but i couldn't find your Library XML file.");
        	//TODO find reason why xml was not found
        	return;
        }
        Map<String,Object> lib = Plist.load(xmlFile);
        
        for(Map.Entry<String,Object> e : lib.entrySet()) {
        	System.out.println(e.getKey()+": "+e.getValue().getClass());
        }
        System.out.println("Fin.");
    }
    
}
