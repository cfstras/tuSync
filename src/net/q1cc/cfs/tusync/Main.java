package net.q1cc.cfs.tusync;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author cfstras
 */
public class Main {

    private static Main inst;
    public SyncGUI gui;
    public TunesManager tunesManager;
    public Preferences props;
    public Converter conv;

    public static final boolean fileSystemCaseSensitive = !(new File( "a" ).equals( new File( "A" )));;

    private Main() {
        props = Preferences.userNodeForPackage(this.getClass());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                try {
                    tunesManager.saveSelectedPlaylists();
                    props.sync();
                } catch (BackingStoreException ex) {
                    ex.printStackTrace();
                }
            }
        });
        conv = new Converter();
    }

    private void init() {
        setPLAF();
        gui = new SyncGUI();
        tunesManager = new TunesManager();
        gui.tunesMan = tunesManager;
        gui.setVisible(true);
    }

    public static Main instance() {
        return inst;
    }

    public static void main(String[] args) {
        inst = new Main();
        inst.init();
    }

    private void setPLAF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
    }
}
