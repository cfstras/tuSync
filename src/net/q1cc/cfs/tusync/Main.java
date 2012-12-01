package net.q1cc.cfs.tusync;

import java.awt.EventQueue;
import java.util.Properties;

/**
 *
 * @author cfstras
 */
public class Main {
    private static Main inst;
    
    public GUI gui;
    public TunesManager tunesManager;
    public Properties props;
    
    private Main(){
       props = new Properties();
        //TODO load props
        
    }
    private void init() {
	gui = new GUI();
        tunesManager = new TunesManager();
        gui.tunesMan=tunesManager;
    }
    public static Main instance() {
        return inst;
    }
    public static void main(String[] args){
        inst = new Main();
        inst.init();
    }
}
