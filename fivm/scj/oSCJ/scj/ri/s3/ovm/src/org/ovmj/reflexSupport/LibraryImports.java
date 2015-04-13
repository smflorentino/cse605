package org.ovmj.reflexSupport;

import org.ovmj.java.Opaque;

class LibraryImports {
    static native void setAllocKind(Class clazz,int index);
    static native Opaque setCurrentArea(int idx,Opaque area);
}

