package net.q1cc.cfs.tusync;

public class TargetFile {
	public final String path;
	public final long size;
	public final long modtime;
	public final long atime;
	
	public TargetFile(String path, long size, long modtime, long atime) {
		this.path = path;
		this.size = size;
		this.modtime = modtime;
		this.atime = atime;
	}
}
