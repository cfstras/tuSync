package net.q1cc.cfs.tusync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.filechooser.FileSystemView;

import jpmp.device.UsbDevice;
import jpmp.manager.DeviceManager;
import jpmp.notifier.IParseTreeNotifier;

public class MTPFSV extends FileSystemView {

    DeviceManager dm;
    
    
    public MTPFSV() throws MTPException {
        try {
            dm = DeviceManager.getInstance();
        } catch (Throwable e) {
            throw new MTPException(e);
        }
        dm.createInstance();
    }
    
    @Override
    public File getChild(File arg0, String arg1) {
        // TODO Auto-generated method stub
        return super.getChild(arg0, arg1);
    }

    @Override
    public File getDefaultDirectory() {
        //TODO
        return null;
    }

    @Override
    public File[] getFiles(File arg0, boolean arg1) {
        // TODO Auto-generated method stub
        return super.getFiles(arg0, arg1);
    }

    @Override
    public File getHomeDirectory() {
        // TODO Auto-generated method stub
        return super.getHomeDirectory();
    }

    @Override
    public File getParentDirectory(File arg0) {
        // TODO Auto-generated method stub
        return super.getParentDirectory(arg0);
    }

    @Override
    public File[] getRoots() {
        long num = dm.scanDevices();
        @SuppressWarnings("unchecked")
        Map<String, UsbDevice> map = dm.getDeviceList();
        for (Entry<String, UsbDevice> e : map.entrySet()) {
            String key = e.getKey();
            UsbDevice dev = e.getValue();
            dev.
        }
    }

    @Override
    public String getSystemDisplayName(File arg0) {
        // TODO Auto-generated method stub
        return super.getSystemDisplayName(arg0);
    }

    @Override
    public boolean isDrive(File dir) {
        // TODO Auto-generated method stub
        return super.isDrive(dir);
    }

    @Override
    public boolean isFileSystem(File arg0) {
        // TODO Auto-generated method stub
        return super.isFileSystem(arg0);
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        // TODO Auto-generated method stub
        return super.isFileSystemRoot(dir);
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        // TODO Auto-generated method stub
        return super.isFloppyDrive(dir);
    }

    @Override
    public boolean isHiddenFile(File f) {
        // TODO Auto-generated method stub
        return super.isHiddenFile(f);
    }

    @Override
    public boolean isParent(File arg0, File arg1) {
        // TODO Auto-generated method stub
        return super.isParent(arg0, arg1);
    }

    @Override
    public boolean isRoot(File arg0) {
        // TODO Auto-generated method stub
        return super.isRoot(arg0);
    }

    @Override
    public Boolean isTraversable(File f) {
        // TODO Auto-generated method stub
        return super.isTraversable(f);
    }

    @Override
    public File createNewFolder(File arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    private class MTPFile extends File {
        UsbDevice dev;
        String pathname;
        boolean isFolder;
        
        public MTPFile(String pathname, UsbDevice dev, boolean isFolder) {
            super(pathname);
            this.pathname = pathname;
            this.dev = dev;
            this.isFolder = isFolder;
        }
        
        @Override public File[] listFiles() {
            final ArrayList<File> files = new ArrayList<>();
            dev.parseFolder(pathname, new IParseTreeNotifier() {
                @Override
                public long addFolder(String path, String name) {
                    files.add(new MTPFile(path + "/" + name, dev, true));
                    return 0;
                }
                @Override
                public long addFile(String path, String name) {
                    files.add(new MTPFile(path + "/" + name, dev, true));
                    return 0;
                }
            });
            return files.toArray(new File[files.size()]);
        }
    }
}
