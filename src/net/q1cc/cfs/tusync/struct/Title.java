/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync.struct;

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
    
    public Title(long id) {
        this.id = id;
        attribs = new Object[attribNames.length];
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
