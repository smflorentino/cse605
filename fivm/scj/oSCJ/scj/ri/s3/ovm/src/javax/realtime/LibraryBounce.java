package javax.realtime;

import org.ovmj.java.Opaque;

public class LibraryBounce {
    private LibraryBounce() {}
    static public Opaque getMemoryAreaMirror(MemoryArea area) {
	return area.area;
    }
    static public MemoryArea getMemoryAreaForMirror(Opaque area) {
	return MemoryArea.getMemoryAreaObject(area);
    }
}

