package java.lang;

import com.fiji.fivm.r1.*;

import java.util.*;

final class VMString {
    private VMString() {}
    
    
    
    static String intern(String str) {
	return FCString.intern(str);
    }
}

