/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
    public HashMap<Integer,Title> titles;

    public TunesManager() {
        main = Main.instance();
        playlists = new ArrayList<Playlist>(32);
        titles = new HashMap<Integer, Title>(256);
    }

    public void loadLibrary() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Main.instance().tunesManager.loadLibraryE();
                } catch (TunesParseException e) {
                    System.out.println("Parse Error: " + e);
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

    public void loadLibraryE() throws XmlParseException, IOException, TunesParseException {
        
        main.gui.progressBar.setIndeterminate(true);

        String path = main.props.getProperty("lib.basepath");
        if (path == null) {
            JOptionPane.showMessageDialog(main.gui.frame, "Please select the path to your iTunes library first.");
            //TODO open dialog
            return;
        }
        File tunesFolder = new File(path);
        File xmlFile = new File(tunesFolder.getAbsolutePath() + System.getProperty("file.separator") + "iTunes Music Library.xml");
        if (!xmlFile.exists()) {
            JOptionPane.showMessageDialog(main.gui.frame, "Sorry, but i couldn't find your Library XML file.");
            //TODO find reason why xml was not found
            return;
        }
        Map<String, Object> lib = Plist.load(xmlFile);

        for (Map.Entry<String, Object> e : lib.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue().getClass());
        }

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
                title.attribs[ind] = e.getValue().toString(); //TODO is this more efficient if we save objects instead of strings?
            }
            titles.put(id, title);
            num++;
            if(num%tracksPerProgress==0 || num==numTracks) {
                main.gui.progressBar.setValue(++progress);
                main.gui.progressBar.setString("reading tracks: "+num+" / "+numTracks);
            }
            
        }
        //tracks loaded.
        
        //playlists!
        
        
        System.out.println("Fin.");
    }
}
