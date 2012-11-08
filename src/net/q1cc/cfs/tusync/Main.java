/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.util.Properties;

/**
 *
 * @author claus
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
        gui.setVisible(true);
        tunesManager = new TunesManager();
    }
    public static Main instance() {
        return inst;
    }
    public static void main(String args){
        inst = new Main();
        
    }
}
