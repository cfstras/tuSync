package net.q1cc.cfs.tusync;

public class Util {

    private static final String siPrefixes = "\0KMGTPEZY";

    public static String humanize(long bytes) {
        int prefixID = 0;
        double divided = bytes;
        while (divided >= 1000 && prefixID < siPrefixes.length()) {
            divided /= 1024;
            prefixID++;
        }
        return (Math.floor(divided * 100) / 100) + " " + siPrefixes.charAt(prefixID) + 'B';
    }
    
    public final static String[] tunesTitleFolders = {
        "Audiobooks", "Automatically Add to iTunes", "Automatisch zu iTunes hinzufügen",
        "iPod Games", "iTunes U", "Mobile Applications", "Movies", "Music", "Podcasts",
        "Ringtones", "Tones", "TV Shows"};
}
