/* UTF8.java - part of FijiRT, derived from GNU Classpath code. */

/* UTF_8.java -- 
   Copyright (C) 2002, 2004, 2005  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package com.fiji.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

/**
 * Simple implementation of Java char to UTF8 (and vice versa) conversion.
 * This class gets used when doing Charset conversions using the java.nio.charset
 * API, but is provided here for convenience, for situations where it would
 * be better to use it directly.
 */
public final class UTF8
{
    private UTF8() {}

    /**
     * Converts UTF8 bytes found in the ByteBuffer to Java characters, which
     * are placed in the CharBuffer.  Returns a java.nio.charset.CoderResult
     * in exactly the same way as CharsetDecoder.decodeLoop(), and almost like
     * CharsetDecoder.decode().  The only difference to the latter is that
     * CharsetDecoder.decode() will report malformed input (rather than
     * underflow) if the endOfInput is set and the input buffer still has
     * data in it; this method does not take an endOfInput parameter and thus
     * may return UNDERFLOW while leaving bytes in the input buffer.
     */
    public static CoderResult decodeLoop (ByteBuffer in, CharBuffer out)
    {
      // TODO: Optimize this in the case in.hasArray() / out.hasArray()
      int inPos = in.position(); 
      try
        {
          while (in.hasRemaining ())
            {
              char c;
              byte b1 = in.get ();
              int highNibble = ((b1 & 0xFF) >> 4) & 0xF;
              switch (highNibble)
                {
                  case 0: case 1: case 2: case 3:
                  case 4: case 5: case 6: case 7:
                    if (out.remaining () < 1)
                      return CoderResult.OVERFLOW;
                    out.put ((char) b1);
                    inPos++;
                    break;		    

                  case 0xC: case 0xD:
                    byte b2;
                    if (in.remaining () < 1)
                      return CoderResult.UNDERFLOW;
                    if (out.remaining () < 1)
                      return CoderResult.OVERFLOW;
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    c = (char) (((b1 & 0x1F) << 6) | (b2 & 0x3F));
                    // check that we had the shortest encoding
                    if (c <= 0x7F)
                      return CoderResult.malformedForLength (2);
                    out.put (c);
                    inPos += 2;
                    break;

                  case 0xE:
                    byte b3;
                    if (in.remaining () < 2)
                      return CoderResult.UNDERFLOW;
                    if (out.remaining () < 1)
                      return CoderResult.OVERFLOW;
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    if (!isContinuation (b3 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    c = (char) (((b1 & 0x0F) << 12)
                                | ((b2 & 0x3F) << 6)
                                | (b3 & 0x3F));
                    // check that we had the shortest encoding
                    if (c <= 0x7FF)
                      return CoderResult.malformedForLength (3);
                    out.put (c);
                    inPos += 3;
                    break;

                  case 0xF:
                    byte b4;
                    if (in.remaining () < 3)
                      return CoderResult.UNDERFLOW;
		    if((b1&0x0F) > 4)
                      return CoderResult.malformedForLength (4);
                    if (out.remaining () < 2)
                      return CoderResult.OVERFLOW;
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (3);
                    if (!isContinuation (b3 = in.get ()))
                      return CoderResult.malformedForLength (2);
                    if (!isContinuation (b4 = in.get ()))
                      return CoderResult.malformedForLength (1);
		    int n = (((b1 & 0x3) << 18)
			     | ((b2 & 0x3F) << 12)
			     | ((b3 & 0x3F) << 6)
			     | (b4 & 0x3F)) - 0x10000;
		    char c1 = (char)(0xD800 | (n & 0xFFC00)>>10);
		    char c2 = (char)(0xDC00 | (n & 0x003FF));
                    out.put (c1);
                    out.put (c2);
                    inPos += 4;
                    break;

                  default:
                    return CoderResult.malformedForLength (1);
                }
            }

          return CoderResult.UNDERFLOW;
        }
      finally
        {
          // In case we did a get(), then encountered an error, reset the
          // position to before the error.  If there was no error, this
          // will benignly reset the position to the value it already has.
          in.position (inPos);
        }
    }
    
    /**
     * Performs a mock UTF8 decoding that calculates exactly how many characters
     * would have been generated.  You can call this prior to calling decodeLoop
     * if you don't want to use more memory than necessary.
     */
    public static CoderResult decodedLengthLoop(ByteBuffer in,
                                                int[] length) {
      // TODO: Optimize this in the case in.hasArray() / out.hasArray()
      int inPos = in.position(); 
      try
        {
          while (in.hasRemaining ())
            {
              char c;
              byte b1 = in.get ();
              int highNibble = ((b1 & 0xFF) >> 4) & 0xF;
              switch (highNibble)
                {
                  case 0: case 1: case 2: case 3:
                  case 4: case 5: case 6: case 7:
                    length[0]++;
                    inPos++;
                    break;		    

                  case 0xC: case 0xD:
                    byte b2;
                    if (in.remaining () < 1)
                      return CoderResult.UNDERFLOW;
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    c = (char) (((b1 & 0x1F) << 6) | (b2 & 0x3F));
                    // check that we had the shortest encoding
                    if (c <= 0x7F)
                      return CoderResult.malformedForLength (2);
                    length[0]++;
                    inPos += 2;
                    break;

                  case 0xE:
                    byte b3;
                    if (in.remaining () < 2)
                      return CoderResult.UNDERFLOW;
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    if (!isContinuation (b3 = in.get ()))
                      return CoderResult.malformedForLength (1);
                    c = (char) (((b1 & 0x0F) << 12)
                                | ((b2 & 0x3F) << 6)
                                | (b3 & 0x3F));
                    // check that we had the shortest encoding
                    if (c <= 0x7FF)
                      return CoderResult.malformedForLength (3);
                    length[0]++;
                    inPos += 3;
                    break;

                  case 0xF:
                    if (in.remaining () < 3)
                      return CoderResult.UNDERFLOW;
		    if((b1&0x0F) > 4)
                      return CoderResult.malformedForLength (4);
                    if (!isContinuation (b2 = in.get ()))
                      return CoderResult.malformedForLength (3);
                    if (!isContinuation (b3 = in.get ()))
                      return CoderResult.malformedForLength (2);
                    if (!isContinuation (in.get ()))
                      return CoderResult.malformedForLength (1);
                    length[0]+=2;
                    inPos += 4;
                    break;

                  default:
                    return CoderResult.malformedForLength (1);
                }
            }

          return CoderResult.UNDERFLOW;
        }
      finally
        {
          // In case we did a get(), then encountered an error, reset the
          // position to before the error.  If there was no error, this
          // will benignly reset the position to the value it already has.
          in.position (inPos);
        }
    }


    private static boolean isContinuation (byte b)
    {
      return (b & 0xC0) == 0x80;
    }


    /**
     * Converts Java characters found in the CharBuffer to UTF8 bytes, which
     * are placed in the ByteBuffer.  Returns a java.nio.charset.CoderResult
     * in exactly the same way as CharsetEncoder.decodeLoop(), and almost like
     * CharsetEncoder.decode().  The only difference to the latter is that
     * CharsetEncoder.decode() will report malformed input (rather than
     * underflow) if the endOfInput is set and the input buffer still has
     * data in it; this method does not take an endOfInput parameter and thus
     * may return UNDERFLOW while leaving bytes in the input buffer.
     */
    public static CoderResult encodeLoop (CharBuffer in, ByteBuffer out)
    {
      int inPos = in.position();
      try
        {
          // TODO: Optimize this in the case in.hasArray() / out.hasArray()
          while (in.hasRemaining ())
          {
            int remaining = out.remaining ();
            char c = in.get ();

            // UCS-4 range (hex.)           UTF-8 octet sequence (binary)
            // 0000 0000-0000 007F   0xxxxxxx
            // 0000 0080-0000 07FF   110xxxxx 10xxxxxx
            // 0000 0800-0000 FFFF   1110xxxx 10xxxxxx 10xxxxxx

            //        Scalar Value          UTF-16                byte 1     byte 2     byte 3     byte 4
            //        0000 0000 0xxx xxxx   0000 0000 0xxx xxxx   0xxx xxxx
            //        0000 0yyy yyxx xxxx   0000 0yyy yyxx xxxx   110y yyyy  10xx xxxx
            //        zzzz yyyy yyxx xxxx   zzzz yyyy yyxx xxxx   1110 zzzz  10yy yyyy  10xx xxxx
            // u uuuu zzzz yyyy yyxx xxxx   1101 10ww wwzz zzyy   1111 0uuu  10uu zzzz  10yy yyyy  10xx xxxx
            //                            + 1101 11yy yyxx xxxx
            // Note: uuuuu = wwww + 1
            if (c <= 0x7F)
              {
                if (remaining < 1)
                  return CoderResult.OVERFLOW;
                out.put ((byte) c);
                inPos++;
              }
            else if (c <= 0x7FF)
              {
                if (remaining < 2)
                  return CoderResult.OVERFLOW;
                out.put ((byte) (0xC0 | (c >> 6)));
                out.put ((byte) (0x80 | (c & 0x3F)));
                inPos++;
              }
            else if (0xD800 <= c && c <= 0xDFFF)
              {
                if (remaining < 4)
                  return CoderResult.OVERFLOW;

                // we got a low surrogate without a preciding high one
                if (c > 0xDBFF)
                  return CoderResult.malformedForLength (1);

                // high surrogates
                if (!in.hasRemaining ())
                  return CoderResult.UNDERFLOW;

                char d = in.get ();

                // make sure d is a low surrogate
                if (d < 0xDC00 || d > 0xDFFF)
                  return CoderResult.malformedForLength (1);

                // make the 32 bit value
                // int value2 = (c - 0xD800) * 0x400 + (d - 0xDC00) + 0x10000;
                int value = (((c & 0x3FF) << 10) | (d & 0x3FF)) + 0x10000;
                // assert value == value2;
                out.put ((byte) (0xF0 | ((value >> 18) & 0x07)));
                out.put ((byte) (0x80 | ((value >> 12) & 0x3F)));
                out.put ((byte) (0x80 | ((value >>  6) & 0x3F)));
                out.put ((byte) (0x80 | ((value      ) & 0x3F)));
                inPos += 2;
              }
            else
              {
                if (remaining < 3)
                  return CoderResult.OVERFLOW;

                out.put ((byte) (0xE0 | (c >> 12)));
                out.put ((byte) (0x80 | ((c >> 6) & 0x3F)));
                out.put ((byte) (0x80 | (c & 0x3F)));
                inPos++;
              }
          }

          return CoderResult.UNDERFLOW;
        }
      finally
        {
          // In case we did a get(), then encountered an error, reset the
          // position to before the error.  If there was no error, this
          // will benignly reset the position to the value it already has.
          in.position (inPos);
        }
    }
    
    /**
     * Performs a mock UTF8 encoding that calculates exactly how many bytes
     * would have been generated.  You can call this prior to calling encodeLoop
     * if you don't want to use more memory than necessary.
     */
    public static CoderResult encodedLengthLoop(CharBuffer in,
                                                int[] length) {
      int inPos = in.position();
      try
        {
          // TODO: Optimize this in the case in.hasArray() / out.hasArray()
          while (in.hasRemaining ())
          {
            char c = in.get ();

            // UCS-4 range (hex.)           UTF-8 octet sequence (binary)
            // 0000 0000-0000 007F   0xxxxxxx
            // 0000 0080-0000 07FF   110xxxxx 10xxxxxx
            // 0000 0800-0000 FFFF   1110xxxx 10xxxxxx 10xxxxxx

            //        Scalar Value          UTF-16                byte 1     byte 2     byte 3     byte 4
            //        0000 0000 0xxx xxxx   0000 0000 0xxx xxxx   0xxx xxxx
            //        0000 0yyy yyxx xxxx   0000 0yyy yyxx xxxx   110y yyyy  10xx xxxx
            //        zzzz yyyy yyxx xxxx   zzzz yyyy yyxx xxxx   1110 zzzz  10yy yyyy  10xx xxxx
            // u uuuu zzzz yyyy yyxx xxxx   1101 10ww wwzz zzyy   1111 0uuu  10uu zzzz  10yy yyyy  10xx xxxx
            //                            + 1101 11yy yyxx xxxx
            // Note: uuuuu = wwww + 1
            if (c <= 0x7F)
              {
                length[0]++;
                inPos++;
              }
            else if (c <= 0x7FF)
              {
                length[0]+=2;
                inPos++;
              }
            else if (0xD800 <= c && c <= 0xDFFF)
              {
                // we got a low surrogate without a preciding high one
                if (c > 0xDBFF)
                  return CoderResult.malformedForLength (1);

                // high surrogates
                if (!in.hasRemaining ())
                  return CoderResult.UNDERFLOW;

                char d = in.get ();

                // make sure d is a low surrogate
                if (d < 0xDC00 || d > 0xDFFF)
                  return CoderResult.malformedForLength (1);
                
                length[0]+=4;
                inPos += 2;
              }
            else
              {
                length[0]+=3;
                inPos++;
              }
          }

          return CoderResult.UNDERFLOW;
        }
      finally
        {
          // In case we did a get(), then encountered an error, reset the
          // position to before the error.  If there was no error, this
          // will benignly reset the position to the value it already has.
          in.position (inPos);
        }
    }
    
    private static void throwInternalError() {
        throw new InternalError("Unexpectedly failed at UTF8 transcoding (this should never happen)");
    }

    /**
     * Completely decodes the bytes into characters using UTF8, applying default
     * replacement policies (i.e. inserting the charcter '\uFFFD') if an error is
     * detected.  This assumes that the chatacter buffer is accurately sized;
     * otherwise an InternalError is thrown.
     */
    public static void decodeCompletely(ByteBuffer in,
                                        CharBuffer out) {
        for (;;) {
            CoderResult cr=decodeLoop(in,out);
            if (cr.isError()) {
                out.put('\uFFFD');
                in.position(in.position()+cr.length());
            } else {
                if (!cr.isUnderflow() || in.hasRemaining()) {
                    throwInternalError();
                }
                return;
            }
        }
    }

    @StackAllocation
    public static int decodedLength(ByteBuffer in) {
        int[] result=new int[1];
        for (;;) {
            CoderResult cr=decodedLengthLoop(in,result);
            if (cr.isError()) {
                result[0]++;
                in.position(in.position()+cr.length());
            } else {
                if (!cr.isUnderflow() || in.hasRemaining()) {
                    throwInternalError();
                }
                return result[0];
            }
        }
    }
    
    @StackAllocation
    public static int decodedLength(byte[] in,int offset,int length) {
        return decodedLength(UTF8Support.wrap(in,offset,length));
    }
    
    @AllocateAsCaller
    public static CharBuffer decode(ByteBuffer in) {
        CharBuffer result=UTF8Support.allocateCharBuffer(decodedLength(in));
        decodeCompletely(in,result);
        if (Settings.ASSERTS_ON && result.hasRemaining()) {
            throwInternalError();
        }
        result.flip();
        return result;
    }
    
    @StackAllocation
    public static void decodeCompletely(byte[] in,int inOffset,int inLength,
                                        char[] out,int outOffset,int outLength) {
        ByteBuffer inBuf=UTF8Support.wrap(in,inOffset,inLength);
        CharBuffer outBuf=UTF8Support.wrap(out,inOffset,outLength);
        decodeCompletely(inBuf,outBuf);
        if (Settings.ASSERTS_ON && inBuf.hasRemaining()) {
            throwInternalError();
        }
        if (Settings.ASSERTS_ON && outBuf.hasRemaining()) {
            throwInternalError();
        }
    }
    
    public static void decodeCompletely(byte[] in,int offset,int length,
                                        char[] out) {
        decodeCompletely(in,offset,length,
                         out,0,out.length);
    }
    
    @StackAllocation
    public static void decodeCompletely(ByteBuffer in,
                                        char[] out) {
        CharBuffer outBuf=UTF8Support.wrap(out,0,out.length);
        decodeCompletely(in,outBuf);
        if (Settings.ASSERTS_ON && in.hasRemaining()) {
            throwInternalError();
        }
        if (Settings.ASSERTS_ON && outBuf.hasRemaining()) {
            throwInternalError();
        }
    }
    
    @AllocateAsCaller
    public static char[] decode(byte[] in,int offset,int length) {
        char[] result=new char[decodedLength(in,offset,length)];
        decodeCompletely(in,offset,length,result);
        return result;
    }
    
    @AllocateAsCaller
    public static char[] decode(byte[] input) {
        return decode(input,0,input.length);
    }
    
    public static void encodeCompletely(CharBuffer in,
                                        ByteBuffer out) {
        for (;;) {
            CoderResult cr=encodeLoop(in,out);
            if (cr.isError()) {
                out.put((byte)'?');
                in.position(in.position()+cr.length());
            } else {
                if (!cr.isUnderflow() || in.hasRemaining()) {
                    throwInternalError();
                }
                return;
            }
        }
    }
    
    @StackAllocation
    public static int encodedLength(CharBuffer in) {
        int[] result=new int[1];
        for (;;) {
            CoderResult cr=encodedLengthLoop(in,result);
            if (cr.isError()) {
                result[0]++;
                in.position(in.position()+cr.length());
            } else {
                if (!cr.isUnderflow() || in.hasRemaining()) {
                    throwInternalError();
                }
                return result[0];
            }
        }
    }
    
    @StackAllocation
    public static int encodedLength(char[] in,int offset,int length) {
        return encodedLength(UTF8Support.wrap(in,offset,length));
    }
    
    @AllocateAsCaller
    public static ByteBuffer encode (CharBuffer in) {
        ByteBuffer result=UTF8Support.allocateByteBuffer(encodedLength(in));
        encodeCompletely(in,result);
        if (Settings.ASSERTS_ON && result.hasRemaining()) {
            throwInternalError();
        }
        result.flip();
        return result;
    }
    
    @StackAllocation
    public static void encodeCompletely(char[] in,int inOffset,int inLength,
                                        byte[] out,int outOffset,int outLength) {
        CharBuffer inBuf=UTF8Support.wrap(in,inOffset,inLength);
        ByteBuffer outBuf=UTF8Support.wrap(out,outOffset,outLength);
        encodeCompletely(inBuf,outBuf);
        if (Settings.ASSERTS_ON && inBuf.hasRemaining()) {
            throwInternalError();
        }
        if (Settings.ASSERTS_ON && outBuf.hasRemaining()) {
            throwInternalError();
        }
    }
    
    public static void encodeCompletely(char[] in,int offset,int length,
                                        byte[] out) {
        encodeCompletely(in,offset,length,
                         out,0,out.length);
    }
    
    @StackAllocation
    public static void encodeCompletely(char[] in,int inOffset,int inLength,
                                        ByteBuffer outBuf) {
        CharBuffer inBuf=UTF8Support.wrap(in,inOffset,inLength);
        encodeCompletely(inBuf,outBuf);
        if (Settings.ASSERTS_ON && inBuf.hasRemaining()) {
            throwInternalError();
        }
    }
    
    @AllocateAsCaller
    public static byte[] encode(char[] in,int offset,int length) {
        byte[] result=new byte[encodedLength(in,offset,length)];
        encodeCompletely(in,offset,length,result);
        return result;
    }

    @AllocateAsCaller
    public static byte[] encode(char[] input) {
        return encode(input,0,input.length);
    }
}
