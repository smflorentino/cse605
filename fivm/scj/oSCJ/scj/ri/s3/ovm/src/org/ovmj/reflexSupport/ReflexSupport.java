package org.ovmj.reflexSupport;

import org.ovmj.java.Opaque;
import javax.realtime.MemoryArea;
import javax.realtime.LibraryBounce;

public class ReflexSupport {
    private ReflexSupport() {}
    
    static public MemoryArea setCurrentArea(int index,
					    MemoryArea area) {
	Opaque oldAreaM=LibraryImports.setCurrentArea(index,area==null?null:LibraryBounce.getMemoryAreaMirror(area));
	if (oldAreaM==null) {
	    return null;
	} else {
	    return LibraryBounce.getMemoryAreaForMirror(oldAreaM);
	}
    }
    
    static public void setAllocKind(Class clazz,int index) {
	LibraryImports.setAllocKind(clazz,index);
    }
}

