package net.q1cc.cfs.tusync.struct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;

import net.q1cc.cfs.tusync.SyncElement;
import net.q1cc.cfs.tusync.SyncTarget;

/**
 *
 * @author claus
 */
public class Playlist extends JCheckBox {
    
    public String title;
    public long id;
    public LinkedHashMap<Long,Title> tracks; //TODO heap? array?
    public boolean selected = false;
    public String parentPersID;
    public String persID;
    
    public Playlist(String title, long id) {
        this.title=title;
        this.id = id;
        tracks = new LinkedHashMap<>(64);
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
    }
    
    public ArrayList<SyncElement> write(SyncTarget target) {
        PrintWriter out = null;
        ArrayList<SyncElement> elements = new ArrayList<>();
        try {
            String filename = title.replaceAll("[^\\w"
                    +Pattern.quote(" äöü-[].{}")+"]", "-").concat(".m3u");
            File playlistFile = new File(targetPathFile.getAbsolutePath()+File.separator+filename);
            System.out.println("Playlist "+title+" --> "+filename);
            playlistFile.createNewFile();
            out = new PrintWriter(new FileWriter(playlistFile, false));

            out.println("#EXTM3U");
            Iterator<Title> titleIt = tracks.values().iterator();
            while(titleIt.hasNext()) {
                Title t = titleIt.next();
                if(!t.selected) {
                    continue;
                }
                SyncElement element = new SyncElement(t);
                Path pathRel = element.destPath;
                if(pathRel == null)
                {
                    continue;
                }
                elements.add(element);

                out.print("#EXTINF:");
                out.print(t.getLength()/100);
                out.print(", ");
                out.print(t.attribs[Title.getAttInd("Artist")]);
                out.print(" - ");
                out.println(t.attribs[Title.getAttInd("Name")]);
                out.println(pathRel);
            }
            out.println();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if(out!=null) {
                out.flush();
                out.close();
            }
        }
        return elements;
    }
}
