// $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/org/ovmj/java/io/BufferingPrintStream.java,v 1.3 2007/05/18 17:42:41 baker29 Exp $

package org.ovmj.java.io;

import gnu.classpath.SystemProperties;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.ovmj.java.Opaque;

/**
 * Implementation of a PrintStream that does its own buffering and can do all
 * print() and println() operations without allocating memory.  (The exception
 * is print(Object) and println(Object), where the user's toString() method
 * may allocate memory.)
 * <p>
 * Note that PrintStream is not an interface and contains a bunch of private
 * fields.  Hence, this class must re-implement everything in PrintStream
 * (including repeating many of those fields).  It's ugly, but that's what's
 * needed to make it work.
 *
 * @author Filip Pizlo
 */
public class BufferingPrintStream extends PrintStream {

    private boolean errorOccurred = false;
    private boolean closed = false;
    private boolean autoFlush;
    private CharsetEncoder encoder;
    private ByteBuffer buffer;
    private int position = 0;
    
    BufferingPrintStream(OutputStream out,
                         boolean autoFlush,
                         CharsetEncoder encoder) {
        super(out);
        this.autoFlush = autoFlush;
	this.encoder = encoder;
        buffer = ByteBuffer.allocate(65536);
    }

    static private CharsetEncoder findEncoder(String enc) {
	if (enc == null) 
	    try {
		enc = SystemProperties.getProperty("file.encoding");
	    } catch (SecurityException e){
		enc = "ISO8859_1";
	    } catch (IllegalArgumentException e){
		enc = "ISO8859_1";
	    } catch (NullPointerException e){
		enc = "ISO8859_1";
	    }
	Charset cs = Charset.forName(enc);
	CharsetEncoder encoder = cs.newEncoder();
        encoder = cs.newEncoder();
	encoder.onMalformedInput(CodingErrorAction.REPLACE);
	encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
	return encoder;
    }

    public BufferingPrintStream(OutputStream out) {
        this(out, false);
    }
    
    public BufferingPrintStream(OutputStream out,
                                boolean autoFlush) {
        this(out, autoFlush, findEncoder(null));
    }
    
    public BufferingPrintStream(OutputStream out,
                                boolean autoFlush,
                                String encoding)
        throws UnsupportedEncodingException {
        this(out,  autoFlush, findEncoder(encoding));
    }
    
    public synchronized boolean checkError() {
        if (!closed) {
            flush();
        }
        
        return errorOccurred;
    }
    
    protected synchronized void setError() {
        errorOccurred=true;
    }
    
    public synchronized void close() {
        try {
            out.close();
        } catch (IOException e) {
            setError();
        }
        closed=true;
    }
    
    public synchronized void flush() {
        try {
            out.write(buffer.array(), 0, buffer.position());
            buffer.position(0);
            out.flush();
        } catch (IOException e) {
            setError();
        }
    }
    
    public synchronized void print(boolean bool) {
	print(bool ? "true" : "false");
    }
    
    public synchronized void print(int inum) {
        Opaque area=LibraryImports.enterScratchPad();
        try {
            print(String.valueOf(inum));
        } finally {
            LibraryImports.leaveArea(area);
        }
    }
    
    public synchronized void print(long lnum) {
        Opaque area=LibraryImports.enterScratchPad();
        try {
            print(String.valueOf(lnum));
        } finally {
            LibraryImports.leaveArea(area);
        }
    }
    
    public synchronized void print(float fnum) {
        Opaque area=LibraryImports.enterScratchPad();
        try {
            print(String.valueOf(fnum));
        } finally {
            LibraryImports.leaveArea(area);
        }
    }
    
    public synchronized void print(double dnum) {
        Opaque area=LibraryImports.enterScratchPad();
        try {
            print(String.valueOf(dnum));
        } finally {
            LibraryImports.leaveArea(area);
        }
    }
    
    public void print(Object obj) {
	print(obj == null ? "null" : obj.toString());
    }
    
    public synchronized void print(String str) {
	Opaque r = LibraryImports.enterScratchPad();
	try {
	    CharBuffer cb = CharBuffer.wrap
	    	(LibraryImports.breakEncapsulation_String_value(str),
		 LibraryImports.breakEncapsulation_String_offset(str),
		 LibraryImports.breakEncapsulation_String_count(str));
	    printImpl(cb);
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }

    public synchronized void print(char c) {
        Opaque area=LibraryImports.enterScratchPad();
        try {
            char[] ca=new char[]{c};
            print(CharBuffer.wrap(ca));
        } finally {
            LibraryImports.leaveArea(area);
        }
    }
    
    void printImpl(CharBuffer cb) {
	cb.mark();
	CoderResult r;
	for (r = encoder.encode(cb, buffer, false);
	     r.isOverflow();
	     r = encoder.encode(cb, buffer, false))
	    flush();
	assert r.isUnderflow(): "completed encoding";

        if (autoFlush) {
	    cb.reset();
            while (cb.hasRemaining()) 
                if (cb.get() == '\n') {
                    flush();
                    return;
                }
        }
    }
    
    public synchronized void print(char[] charArray) {
	Opaque r = LibraryImports.enterScratchPad();
	try {
	    printImpl(CharBuffer.wrap(charArray));
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }
    
    public synchronized void println() {
        write('\n');
    }
    
    public synchronized void println(boolean bool) {
	print(bool ? "true\n" : "false\n");
    }
    
    public synchronized void println(int inum) {
        print(inum);
        write('\n');
    }
    
    public synchronized void println(long lnum) {
        print(lnum);
        write('\n');
    }
    
    public synchronized void println(float fnum) {
        print(fnum);
        write('\n');
    }
    
    public synchronized void println(double dnum) {
        print(dnum);
        write('\n');
    }
    
    public synchronized void println(Object obj) {
	print(obj);
	write('\n');
    }
    
    public synchronized void println(String str) {
        print(str);
        write('\n');
    }
    
    public synchronized void println(char ch) {
        print(ch);
        write('\n');
    }
    
    public synchronized void println(char[] charArray) {
        print(charArray);
        write('\n');
    }
    
    public synchronized void write(int oneByte) {
        if (!buffer.hasRemaining()) {
            flush();
        }
	buffer.put((byte)oneByte);
        if (autoFlush && oneByte=='\n') {
            flush();
        }
    }
    
    public synchronized void write(byte[] data,
                                   int dataOffset,
                                   int dataLen) {
        if (dataLen>buffer.remaining()) {
            flush();
        }
        
        if (dataLen>buffer.capacity()) {
            try {
                out.write(data,dataOffset,dataLen);
                out.flush();
            } catch (IOException e) {
                setError();
            }
            return;
        }
        buffer.put(data, dataOffset, dataLen);

        if (autoFlush) { // always flush on byte[]
            flush();
        }
    }
}

