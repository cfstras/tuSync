/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.tusync;

/**
 *
 * @author Claus
 */
public class Values {
    
    public static String[] iTunesTypes = {
        "--Nope--",
        "MPEG-Audiodatei",
        "AAC-Audiodatei",
        "Apple Lossless-Audiodatei",
        "AAC-Audio-Stream",
        "Internetaudio-Stream",
        "Gekaufte AAC-Audiodatei",
        "MPEG-Audio-Stream",
        "Geschützte AAC-Audiodatei",
        "QuickTime Filmdatei",
        "WAV-Audiodatei",
        "Audible-Datei",
    };
    
    public static String[] encoders = {
        "-acodec flac -aq 8",
    };
    
    // E:\testDevice>ffmpeg -y -v 31 -vn -i "Feeling Good.m4a" -acodec flac -aq 8 "Feeling Good.flac"
    
}
