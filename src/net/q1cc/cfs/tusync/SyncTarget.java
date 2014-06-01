package net.q1cc.cfs.tusync;

import java.util.Collection;

public interface SyncTarget {
	public void addFiles(Collection<SyncElement> files);
	public void startSync();
	public float getProgress();
	public String getState();
	public void waitFinish();
	public void deleteOldPlaylists();
}
