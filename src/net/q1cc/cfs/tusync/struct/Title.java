package net.q1cc.cfs.tusync.struct;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
        "Sort Series", "Podcast", "Rating Computed"
    }; //TODO find out names
    //TODO ignore some attributes?

    public final static HashMap<String,Integer> attribIndexes;
    
    static {
        attribIndexes = new HashMap<>(attribNames.length);
        for(int i=0;i<attribNames.length;i++) {
            attribIndexes.put(attribNames[i], i);
        }
    }
    
    public static int getAttInd(String name) {
        Integer i = attribIndexes.get(name);
        if(i==null) {
            System.out.println("ERROR: could not find attribute for "+name);
            return -1;
        }
        return i;
    }
    
    public long id;
    public Object[] attribs;
    public boolean selected;
    public volatile boolean fileChecked;
    public static volatile Path baseFolder;
    
    public Title(long id) {
        this.id = id;
        attribs = new Object[attribNames.length];
    }

    private boolean find() {
        Object kind = attribs[getAttInd("Kind")];
        if(kind != null && kind instanceof String && ((String)kind).endsWith("Stream")) {
            selected = false;
            attribs[getAttInd("Size")] = 0L;
            fileChecked = true;
            return false;
        }
        Object loc = attribs[getAttInd("Location")];
        if(loc == null) {
            System.out.println("no location for "+toString());
            attribs[getAttInd("Size")] = 0L;
            fileChecked = true;
            return false;
        }
        if(!(loc instanceof String)) {
            System.out.println("location not valid "+toString());
            attribs[getAttInd("Size")] = 0L;
            fileChecked = true;
            return false;
        }

        if(((String)loc).startsWith("file://localhost/")) {
            loc = "file:///"+((String)loc).substring("file://localhost/".length());
        }

        Path p = new File(URI.create((String)loc)).toPath();
        if(!Files.exists(p)) {
            System.out.println("not found "+p.toAbsolutePath()+" in "+toString());
            attribs[getAttInd("Size")] = 0L;
            fileChecked = true;
            return false;
        }
        attribs[getAttInd("Location")] = p.toAbsolutePath();
        fileChecked = true;
//        if(!Files.isReadable(p)) {
        if(!p.toFile().canRead()) {
            System.out.println("can't read: "+this);
            attribs[getAttInd("Size")] = 0L;
            return false;
        }
        long size;
        try {
            size = Files.size(p);
            attribs[getAttInd("Size")] = size;
            return true;
        } catch (IOException ex) {
            System.err.println(ex); //TODO handle gracefully
        }
        selected = false;
        return false;

    }

    public long getSizeOnDisk() {
        if(!fileChecked) {
            find();
        }
        long l = (Long)attribs[getAttInd("Size")];
            if(l==0) {
                selected=false;
            }
        return l;
    }

    public Path getFile() throws IOException {
        if(!fileChecked) {
            find();
        }
        return (Path)attribs[getAttInd("Location")];
    }
    
    public long getLength() {
        Object l = attribs[getAttInd("Total Time")];
        if(l instanceof Integer) {
            return (Integer)l;
        }
        if(l instanceof Long) {
            return (Long)l;
        }
        System.out.println("time: "+l+" for "+toString());
        return -1;
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

    public static String decodeLocation(String location) {
        if(location.startsWith("file://localhost/")) {
            location = location.substring("file://localhost/".length());
        }
        try {
            return URLDecoder.decode(location.replace("+","%2b"),"UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return location;
    }
    
    @Deprecated
    public Path getPathRelative() throws IOException {
        Path pathAbsolute = getFile();
        if(pathAbsolute == null)
        {
            return null;
        }
        Path without = baseFolder.relativize(pathAbsolute);
        return without;
    }

    /**
     * fetches the relative destination Path of the Title, usually Artist/Album/Title.
     * the file extension is preserved from the source file.
     * @return
     */
    public Path getDestRelative() {
        if(!fileChecked) {
            find();
        }
        Path p = (Path)attribs[getAttInd("Location")];
        //check if we are below the base path
        Path rel = baseFolder.relativize(p);
        if(rel.startsWith("..")) {
            // base path is outside of main path, construct one.
            //TODO use a name scheme here
            rel = p.subpath(p.getNameCount()-3,p.getNameCount());
            //TODO check if we need to convert
        }
        
        return rel;
    }
}
