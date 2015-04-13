/*
 * NioReader.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package com.fiji.fivm.c1;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

// FIXME: could have a parallel version of this that takes advantage of the random
// access capabilities of the underlying channel, and reads a buffer at a time,
// giving each buffer to a thread.

public class NioReader {
    String filename;
    FileChannel channel;
    ByteBuffer buffer;
    int cnt=0;
    
    public NioReader(String filename) throws IOException {
        this.filename=filename;
        channel=new RandomAccessFile(filename,"r").getChannel();
        
        buffer=ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        readCompletely();
        buffer.flip();
        
        int maxBufSize=buffer.getInt();
        if (maxBufSize<0) {
            throw new CompilerException("Attempting to read incompletely written file: "+filename);
        }
        buffer=ByteBuffer.allocate(maxBufSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        buffer.limit(0);
    }
    
    public static int readCompletely(ReadableByteChannel channel,
                                     ByteBuffer buffer) throws IOException {
        int position=buffer.position();
        while (buffer.remaining()>0) {
            if (channel.read(buffer)<0) {
                break;
            }
        }
        return buffer.position()-position;
    }
    
    private int readCompletely() {
        try {
            return readCompletely(channel,buffer);
        } catch (Throwable e) {
            throw new CompilerException("Could not read from "+filename,e);
        }
    }
    
    private boolean soak() {
        buffer.clear();
        buffer.limit(4);
        if (readCompletely()==0) {
            close();
            return false;
        } else {
            buffer.position(0);
            int amount=buffer.getInt();
            buffer.clear();
            buffer.limit(amount);
            if (readCompletely()<amount) {
                throw new CompilerException("Incomplete read from "+filename);
            }
            buffer.position(0);
            buffer.limit(amount);
            return true;
        }
    }
    
    public synchronized boolean read(NioReadable obj) {
        try {
            if (channel==null ||
                (buffer.remaining()==0 &&
                 !soak())) {
                return false;
            }
            
            obj.readFrom(buffer);
            cnt++;
            return true;
        } catch (Throwable e) {
            try {
                throw new CompilerException("Could not read from "+filename+" after reading "+cnt+" objects; error around offset "+(channel.position()-buffer.remaining()),e);
            } catch (IOException e2) {
                e.printStackTrace(Global.log);
                throw new CompilerException("Could not get channel position on "+filename+" while attempting to report another error",e2);
            }
        }
    }
    
    public synchronized void close() {
        try {
            if (channel!=null) {
                channel.close();
                channel=null;
                buffer=null;
            }
        } catch (Throwable e) {
            throw new CompilerException("Could not close "+filename,e);
        }
    }
}


