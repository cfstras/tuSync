/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

import java.nio.file.Path;
import java.util.Properties;
import net.q1cc.cfs.tusync.struct.Title;

/**
 *
 * @author Claus
 */
public class Converter {
    Main main;

    public boolean isDoConvert() {
        return doConvert;
    }

    public void setDoConvert(boolean doConvert) {
        this.doConvert = doConvert;
    }

    public String getConvFrom() {
        return convFrom;
    }

    public void setConvFrom(String convFrom) {
        this.convFrom = convFrom;
    }

    public String getConvTo() {
        return convTo;
    }

    public void setConvTo(String convTo) {
        this.convTo = convTo;
    }
    
    private boolean doConvert;
    private String convFrom;
    private String convTo;
    
    public Converter() {
        main = Main.instance();
        
    }
    
    /**
     * checks whether the specified title should undergo conversion.
     * @param t
     * @return 
     */
    public boolean shouldConvert(Title src) {
        if(doConvert && src.attribs[Title.getAttInd("Kind")].equals(convFrom)) {
            return true;
        }
        return false;
    }
    
    /**
     * convert() takes a Title object, converts its data and stores the result
     * in the Path given.
     * @param src
     * @return whether the converting was successful
     */
    public boolean convert(Title src, Path dest) {
        if (!shouldConvert(src)) {
            return false;
        }
        
        
        return false;
    }
}
