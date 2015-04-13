/* ByteBufferImpl.java -- 
   Copyright (C) 2002, 2003, 2004, 2005  Free Software Foundation, Inc.

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


package java.nio;

import __vm.FCMagic;
import __vm.FCPtr;
import com.fiji.fivm.r1.*;

/**
 * This is a Heap memory implementation
 */
final class ByteBufferImpl extends ByteBuffer
{
  final boolean readOnly;

  ByteBufferImpl (byte[] buffer,
		  FCPtr address,
		  int offset,
		  int capacity,
		  int limit,
		  int position,
		  int mark,
		  boolean readOnly)
  {
    super (capacity, limit, position, mark);
    this.backing_buffer = buffer;
    this.array_offset = offset;
    this.readOnly = readOnly;
    this.address = address;
    if (address==FCMagic.zero()) {
        if (capacity==0) {
            this.address = FCMagic.zero();
            this.contiguous_ = true;
        } else {
            this.address = FCMagic.addressOfElement(buffer, offset);
            
            if (!FCMagic.allContiguous()) {
                if (backing_buffer==null) {
                    this.contiguous_ = true;
                } else {
                    this.contiguous_ = FCMagic.isContiguous(buffer);
                    if (!contiguous_) {
                        this.address = FCMagic.zero();
                    }
                }
            }
        }
    } else {
        this.contiguous_ = true;
        if (buffer!=null && !FCMagic.isContiguous(buffer)) {
            throw new Error("fail"); // should never happen
        }
    }
  }
  
  public CharBuffer asCharBuffer ()
  {
    return new CharViewBufferImpl (this, remaining() >> 1);
  }

  public ShortBuffer asShortBuffer ()
  {
    return new ShortViewBufferImpl (this, remaining() >> 1);
  }

  public IntBuffer asIntBuffer ()
  {
    return new IntViewBufferImpl (this, remaining() >> 2);
  }

  public LongBuffer asLongBuffer ()
  {
    return new LongViewBufferImpl (this, remaining() >> 3);
  }

  public FloatBuffer asFloatBuffer ()
  {
    return new FloatViewBufferImpl (this, remaining() >> 2);
  }

  public DoubleBuffer asDoubleBuffer ()
  {
    return new DoubleViewBufferImpl (this, remaining() >> 3);
  }

  @Inline
  public boolean isReadOnly ()
  {
    return readOnly;
  }
  
  public ByteBuffer slice ()
  {
      return sliceHelper();
  }

  public ByteBuffer duplicate ()
  {
    return duplicateHelper();
  }

  public ByteBuffer asReadOnlyBuffer ()
  {
    return asReadOnlyBufferHelper();
  }

  public ByteBuffer compact ()
  {
    return compactHelper();
  }

  @Inline
  public boolean isDirect ()
  {
      return isVMBuffer();
  }

  /**
   * Reads the <code>byte</code> at this buffer's current position,
   * and then increments the position.
   *
   * @exception BufferUnderflowException If there are no remaining
   * <code>bytes</code> in this buffer.
   */
  @Inline
  public byte get ()
  {
    return getHelper();
  }

/**
   * Bulk get
   */
  @Inline
  public ByteBuffer get (byte[] dst, int offset, int length)
  {
    return getHelper(dst, offset, length);
  }

/**
   * Relative bulk put(), overloads the ByteBuffer impl.
   */
  @Inline
  public ByteBuffer put (byte[] src, int offset, int length)
  {
    return putHelper(src, offset, length);
  }

/**
   * Relative put method. Writes <code>value</code> to the next position
   * in the buffer.
   *
   * @exception BufferOverflowException If there is no remaining
   * space in this buffer.
   * @exception ReadOnlyBufferException If this buffer is read-only.
   */
  @Inline
  public ByteBuffer put (byte value)
  {
      return putHelper(value);
  }

/**
   * Absolute get method. Reads the <code>byte</code> at position
   * <code>index</code>.
   *
   * @exception IndexOutOfBoundsException If index is negative or not smaller
   * than the buffer's limit.
   */
  @Inline
  public byte get (int index)
  {
    return getHelper(index);
  }

/**
   * Absolute put method. Writes <code>value</code> to position
   * <code>index</code> in the buffer.
   *
   * @exception IndexOutOfBoundsException If index is negative or not smaller
   * than the buffer's limit.
   * @exception ReadOnlyBufferException If this buffer is read-only.
   */
  @Inline
  public ByteBuffer put (int index, byte value)
  {
    return putHelper(index, value);
  }
    
  @Inline
  public ByteBuffer put (ByteBuffer src) {
      return putHelper(src);
  }

  @Inline
  public char getChar ()
  {
    return ByteBufferHelper.getChar(this, order());
  }
  
  @Inline
  public ByteBuffer putChar (char value)
  {
      ByteBufferHelper.putChar(this, value, order());
      return this;
  }
  
  @Inline
  public char getChar (int index)
  {
    return ByteBufferHelper.getChar(this, index, order());
  }
  
  @Inline
  public ByteBuffer putChar (int index, char value)
  {
    ByteBufferHelper.putChar(this, index, value, order());
    return this;
  }

  @Inline
  public short getShort ()
  {
    return ByteBufferHelper.getShort(this, order());
  }
  
  @Inline
  public ByteBuffer putShort (short value)
  {
    ByteBufferHelper.putShort(this, value, order());
    return this;
  }
  
  @Inline
  public short getShort (int index)
  {
    return ByteBufferHelper.getShort(this, index, order());
  }
  
  @Inline
  public ByteBuffer putShort (int index, short value)
  {
    ByteBufferHelper.putShort(this, index, value, order());
    return this;
  }

  @Inline
  public int getInt ()
  {
    return ByteBufferHelper.getInt(this, order());
  }
  
  @Inline
  public ByteBuffer putInt (int value)
  {
    ByteBufferHelper.putInt(this, value, order());
    return this;
  }
  
  @Inline
  public int getInt (int index)
  {
    return ByteBufferHelper.getInt(this, index, order());
  }
  
  @Inline
  public ByteBuffer putInt (int index, int value)
  {
    ByteBufferHelper.putInt(this, index, value, order());
    return this;
  }

  @Inline
  public long getLong ()
  {
    return ByteBufferHelper.getLong(this, order());
  }
  
  @Inline
  public ByteBuffer putLong (long value)
  {
    ByteBufferHelper.putLong (this, value, order());
    return this;
  }
  
  @Inline
  public long getLong (int index)
  {
    return ByteBufferHelper.getLong (this, index, order());
  }
  
  @Inline
  public ByteBuffer putLong (int index, long value)
  {
    ByteBufferHelper.putLong (this, index, value, order());
    return this;
  }

  @Inline
  public float getFloat ()
  {
    return ByteBufferHelper.getFloat (this, order());
  }
  
  @Inline
  public ByteBuffer putFloat (float value)
  {
    ByteBufferHelper.putFloat (this, value, order());
    return this;
  }
  
  @Inline
  public float getFloat (int index)
  {
    return ByteBufferHelper.getFloat (this, index, order());
  }

  @Inline
  public ByteBuffer putFloat (int index, float value)
  {
    ByteBufferHelper.putFloat (this, index, value, order());
    return this;
  }

  @Inline
  public double getDouble ()
  {
    return ByteBufferHelper.getDouble (this, order());
  }

  @Inline
  public ByteBuffer putDouble (double value)
  {
    ByteBufferHelper.putDouble (this, value, order());
    return this;
  }
  
  @Inline
  public double getDouble (int index)
  {
    return ByteBufferHelper.getDouble (this, index, order());
  }
  
  @Inline
  public ByteBuffer putDouble (int index, double value)
  {
    ByteBufferHelper.putDouble (this, index, value, order());
    return this;
  }
    
  @Inline
  boolean isVMBuffer() {
      return contiguous();
  }
}
