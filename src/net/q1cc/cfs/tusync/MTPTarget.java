package net.q1cc.cfs.tusync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.filechooser.FileSystemView;

import jpmp.manager.DeviceManager;

public class MTPTarget implements SyncTarget {

    ArrayList<SyncElement> files;
    
    public MTPTarget() {
        this.files = new ArrayList<SyncElement>();
    }
    
    public static Map listTargets() {
        try {
            DeviceManager dm = DeviceManager.getInstance();
            dm.createInstance();
            dm.scanDevices();
            return dm.getDeviceList();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public FileSystemView getFSV() {
        return new MTPFSV();
    }
    
    @Override
    public void addFiles(Collection<SyncElement> files) {
        this.files.addAll(files);
    }

    @Override
    public void startSync() {
        // TODO Auto-generated method stub
        
    }
    
    private void doSync() {
        //TODO
    }

    @Override
    public float getProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void waitFinish() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteOldPlaylists() {
        // TODO Auto-generated method stub
        
    }

}
