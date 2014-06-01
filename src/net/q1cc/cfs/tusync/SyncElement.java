package net.q1cc.cfs.tusync;

import java.nio.file.Path;

import net.q1cc.cfs.tusync.struct.Title;

public class SyncElement {
    public final Title title;
    public final Path destPath;
    public final long size;

    public SyncElement(Title title) {
        destPath = title.getDestRelative();
        this.title = title;
        this.size = title.getSizeOnDisk();
    }
}
