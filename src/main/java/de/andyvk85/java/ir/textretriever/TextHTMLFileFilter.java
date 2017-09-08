package de.andyvk85.java.ir.textretriever;

import java.io.File;
import java.io.FileFilter;

public class TextHTMLFileFilter implements FileFilter {

    public boolean accept(File f) {
        String filename = f.getName().toLowerCase();

        if(filename.endsWith(".txt") || filename.endsWith(".html"))
            return true;
        else
            return false;
    }
}