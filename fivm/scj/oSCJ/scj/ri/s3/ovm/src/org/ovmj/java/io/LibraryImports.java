/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/org/ovmj/java/io/LibraryImports.java,v 1.1 2004/10/15 01:53:12 dholmes Exp $
 */
package org.ovmj.java.io;

import org.ovmj.java.Opaque;
class LibraryImports {
    static native Opaque enterScratchPad();
    static native void leaveArea(Opaque prevArea);
    
    static native char[] breakEncapsulation_String_value(String str);
    static native int breakEncapsulation_String_count(String str);
    static native int breakEncapsulation_String_offset(String str);
}
