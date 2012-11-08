/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync.struct;

/**
 *
 * @author claus
 */
public class Title {
    public final static String[] attribNames = {""}; //TODO find out names
    
    public long id;
    public String[] attribs;
    
    public Title(long id) {
        this.id = id;
        attribs = new String[attribNames.length];
    }
}
