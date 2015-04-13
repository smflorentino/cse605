/*
 * NioWriter.java
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

public class NioWriter {
    static private int INIT_BUF_SIZE=4096;
    
    String filename;
    FileChannel channel;
    ByteBuffer smallBuf;
    ByteBuffer buffer;
    
    public NioWriter(String filename) throws IOException {
        this.filename=filename;
        channel=new RandomAccessFile(filename,"rw").getChannel();
        
        smallBuf=ByteBuffer.allocate(4);
        smallBuf.order(ByteOrder.LITTLE_ENDIAN);
        buffer=ByteBuffer.allocate(INIT_BUF_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        smallBuf.putInt(-1);
        smallBuf.flip();
        writeCompletely(smallBuf);
    }
    
    public static void writeCompletely(WritableByteChannel channel,
                                       ByteBuffer buffer) throws IOException {
        while (buffer.remaining()>0) {
            channel.write(buffer);
        }
    }
    
    private void writeCompletely(ByteBuffer buffer) {
        try {
            writeCompletely(channel,buffer);
        } catch (Throwable e) {
            throw new CompilerException("Could not write to "+filename,e);
        }
    }
    
    public synchronized void flush() {
        buffer.flip();
        if (buffer.remaining()>0) {
            smallBuf.clear();
            smallBuf.putInt(buffer.remaining());
            smallBuf.flip();
            writeCompletely(smallBuf);
            writeCompletely(buffer);
        }
        buffer.clear();
    }
    
    public static ByteBuffer writeToAndResize(NioWritable obj,
                                              ByteBuffer buf) {
        if (buf==null) {
            buf=ByteBuffer.allocate(INIT_BUF_SIZE);
            buf.order(ByteOrder.LITTLE_ENDIAN);
        }
        try {
            obj.writeTo(buf);
        } catch (BufferOverflowException e2) {
            for (;;) {
                buf=ByteBuffer.allocate(buf.capacity()*2);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                try {
                    obj.writeTo(buf);
                    break;
                } catch (BufferOverflowException e3) {
                    // retry
                }
            }
        }
        return buf;
    }
    
    // this could be optimizified.
    public synchronized void write(NioWritable obj) {
        try {
            int position=buffer.position();
            try {
                obj.writeTo(buffer);
            } catch (BufferOverflowException e) {
                buffer.position(position);
                flush();
                buffer=writeToAndResize(obj,buffer);
            }
        } catch (Throwable e) {
            throw new CompilerException("Could not write to "+filename,e);
        }
    }
    
    public synchronized void close() {
        try {
            if (channel!=null) {
                flush();
                channel.position(0);
                smallBuf.clear();
                smallBuf.putInt(buffer.capacity());
                smallBuf.flip();
                writeCompletely(smallBuf);
                channel.close();
                channel=null;
                smallBuf=null;
                buffer=null;
            }
        } catch (Throwable e) {
            throw new CompilerException("Could not write to "+filename,e);
        }
    }
}

