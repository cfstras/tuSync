package net.q1cc.cfs.tusync;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JPanel;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JTree;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JProgressBar;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class GUI {

    JFrame frame;
    private JTextField libPathField;
    JProgressBar progressBar;
    JTree tree;

    /**
     * Create the application.
     */
    public GUI() {
        initialize();
        frame.setVisible(true);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("tuSync");
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel top = new JPanel();
        frame.getContentPane().add(top, BorderLayout.NORTH);
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

        JLabel lblItunesDirectory = new JLabel("iTunes Directory");
        top.add(lblItunesDirectory);

        Component horizontalStrut = Box.createHorizontalStrut(30);
        top.add(horizontalStrut);

        libPathField = new JTextField();
        libPathField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Main.instance().props.setProperty("lib.basepath", GUI.this.libPathField.getText());
            }
        });
        top.add(libPathField);
        libPathField.setColumns(10);

        JButton btnChoose = new JButton("Choose");
        btnChoose.addActionListener(new ActionListener() {
            @SuppressWarnings("serial")
            public void actionPerformed(ActionEvent e) {
                File home = new File(System.getProperty("user.home"));
                File tunes = new File(home.toString()+"/Music/iTunes/");
                if(tunes.exists()) {
                    home = tunes;
                }
                JFileChooser jfc = new JFileChooser(home) {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().equals("iTunes Music Library.xml");
                    }
                };
                jfc.showOpenDialog(GUI.this.frame);
                File f = jfc.getSelectedFile();
                if (f != null) {
                    GUI.this.libPathField.setText(f.getAbsolutePath());
                    Main.instance().props.setProperty("lib.xmlfile", f.getAbsolutePath());
                    Main.instance().props.setProperty("lib.basepath", f.getParent());
                } else {
                    JOptionPane.showMessageDialog(GUI.this.frame, "You did not select anything! Why would you do that to me?");
                }
            }
        });
        top.add(btnChoose);

        tree = new JTree();
        tree.setModel(new DefaultTreeModel(
                new DefaultMutableTreeNode("Please select your Library Folder.") {
                    {
                    }
                }));
        tree.setPreferredSize(new Dimension(500, 600));
        frame.getContentPane().add(tree, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        JButton btnLoadDB = new JButton("Load DB");
        btnLoadDB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Main.instance().tunesManager.loadLibrary();
            }
        });
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(btnLoadDB);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(progressBar);

        JButton btnStartSyncing = new JButton("Start Syncing");
        panel.add(btnStartSyncing);
        frame.setLocationByPlatform(true);
        frame.pack();
    }
}
