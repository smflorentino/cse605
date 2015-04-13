package com.fiji.rt;

import java.io.*;

public class RTOSFilesystem {
    File imageDirectory;
    int maxFileDescriptors;
    
    public RTOSFilesystem() {
	this.imageDirectory=null;
	this.maxFileDescriptors=20;
    }
    
    public File getImageDirectory() {
	return imageDirectory;
    }
    
    public void setImageDirectory(File dir) {
	if (!dir.isDirectory()) {
	    throw new IllegalArgumentException(
		"Invalid argument for imageDirectory: "+dir);
	}
	this.imageDirectory=dir;
    }
    
    public int getMaxFileDescriptors() {
	return maxFileDescriptors;
    }
    
    public void setMaxFileDescriptors(int maxFileDescriptors) {
	if (maxFileDescriptors<1 || maxFileDescriptors>65536) {
	    throw new IllegalArgumentException(
		"Invalid argument for maxFileDescriptors: "+maxFileDescriptors);
	}
	this.maxFileDescriptors=maxFileDescriptors;
    }
}


