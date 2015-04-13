/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.realtime;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import javax.safetycritical.annotate.Level;


import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * An interface that facilitates access to raw memory using with scalar data types.
 *  Implementation-defined objects that implement this interface police access to 
 *  a con- tiguous area of physical memory. Offsets are used to access instance or 
 *  arrays of the primitive Java scalar types.
 *  SCJ supports this interface in its entirety. 
 * @author plsek
 *
 */
@SCJAllowed(LEVEL_1)
public interface RawIntegralAccess {

	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public byte getByte(long offset);
/*         throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void getBytes(long offset, byte[] bytes, int low, int number);
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public int getInt(long offset);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void getInts(long offset, int[] ints, int low, int number);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
	@SCJAllowed(LEVEL_1) 
  //@SCJRestricted{InterruptSafe}
  public long getLong(long offset);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
	@SCJAllowed(LEVEL_1) 
 // @SCJRestricted{InterruptSafe}
  public void getLongs(long offset, long[] longs, int low, int number);
 /*        throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public short getShort(long offset);
 /*        throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
                
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void getShorts(long offset, short[] shorts, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
      
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe} 
  public void setByte(long offset, byte value);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
	@SCJAllowed(LEVEL_1) 
  //@SCJRestricted{InterruptSafe}    
  public void setBytes(long offset, byte[] bytes, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void setInt(long offset, int value);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
  
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void setInts(long offset, int[] its, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void setByte(long offset, long value);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
     
	@SCJAllowed(LEVEL_1) 
  //@SCJRestricted{InterruptSafe}
  public void setLongs(long offset, long[] longs, int low, int number) /*throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/;
  
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe}
  public void setShort(long offset, short value) /*throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/;
  
	@SCJAllowed(LEVEL_1)
  //@SCJRestricted{InterruptSafe} 
  public void setShorts(long offset, short[] shorts, int low, int number) /* throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/;

    public void close();
}
