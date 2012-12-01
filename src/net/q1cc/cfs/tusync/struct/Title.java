/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync.struct;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 *
 * @author claus
 */
public class Title {
    public final static String[] attribNames = {"Track ID", "Persistent ID",
        "Album Rating Computed", "Kind", "Year", "File Folder Count", "Genre",
        "Date Modified", "Album Rating", "Artist", "Play Date", "Album", "Size",
        "Play Date UTC", "Comments", "Sample Rate", "Location", "Bit Rate",
        "Date Added", "Album Artist", "Name", "Library Folder Count", "Track Type",
        "Total Time", "Play Count","Skip Date", "Skip Count", "Track Number",
        "Artwork Count","Composer", "Track Count", "Disc Number", "Rating",
        "Sort Album", "Compilation", "BPM", "Volume Adjustment", "Sort Name",
        "Sort Artist", "Sort Album Artist", "Disc Count", "Grouping", "HD",
        "Video Height", "Video Width", "Movie", "Has Video", "TV Show", "Series",
        "Season", "Start Time", "Sort Composer", "Episode Order", "Content Rating",
        "Release Date", "Protected", "Purchased", "Episode", "Stop Time",
        "Equalizer", "Unplayed", "iTunesU", "Part Of Gapless Album", "Explicit",
        "Sort Series"
    }; //TODO find out names
    
    public final static HashMap<String,Integer> attribIndexes;
    
    static {
        HashMap<String,Integer> inds = new HashMap<String, Integer>(attribNames.length);
        for(int i=0;i<attribNames.length;i++) {
            inds.put(attribNames[i], i);
        }
        attribIndexes = inds;
    }
    
    public static int getAttInd(String name) {
        for(int i=0;i<attribNames.length;i++) {
            if(attribNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
    public long id;
    public Object[] attribs;
    public boolean selected;
    public File file;
    
    public Title(long id) {
        this.id = id;
        attribs = new Object[attribNames.length];
    }
    
    public long getSizeOnDisk() {
        Object kind = attribs[attribIndexes.get("Kind")];
        if(kind != null && kind instanceof String && ((String)kind).endsWith("Stream")) {
            selected = false;
            return 0;
        }
        Object loc = attribs[attribIndexes.get("Location")];
        if(loc == null) {
            System.out.println("no location for "+toString());
            return 0;
        }
        if(!(loc instanceof String)) {
            System.out.println("location not valid "+toString());
            return 0;
        }
        
        String location = (String)loc; 
        if(location.startsWith("file://localhost/")) {
            location = location.substring("file://localhost/".length());
        }
        location = location.replaceAll("\\+","%2b");
        File f;
        try {
            f = new File(URLDecoder.decode(location,"UTF-8"));
        } catch (Exception ex) {
            System.out.println(location);
            ex.printStackTrace();
            return 0;
        }
        if(!f.exists()) {
            System.out.println("404 "+f.getAbsolutePath()+" in "+toString());
            return 0;
        }
        if(!f.canRead()) {
            System.out.println("403 "+toString());
            return 0;
        }
        file = f;
        long size = f.length();
        if(!new Long(size).equals( attribs[attribIndexes.get("Size")] )) {
            attribs[attribIndexes.get("Size")] = size;
        }
        return size;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("[");
        for(int i=0;i<attribNames.length;i++) {
            b.append(attribNames[i]).append(":").append(attribs[i]).append(";");
        }
        b.append("]");
        return b.toString();
    }
}
