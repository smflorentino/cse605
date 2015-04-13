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

package gnu.java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import com.fiji.util.UTF8;

/**
 * UTF-8 charset.
 * 
 * <p> UTF-8 references:
 * <ul>
 *   <li> <a href="http://ietf.org/rfc/rfc2279.txt">RFC 2279</a>
 *   <li> The <a href="http://www.unicode.org/unicode/standard/standard.html">
 *     Unicode standard</a> and 
 *     <a href="http://www.unicode.org/versions/corrigendum1.html">
 *      Corrigendum</a>
 * </ul>
 *
 * @author Jesse Rosenstock
 */
final class UTF_8 extends Charset
{
  UTF_8 ()
  {
    super ("UTF-8", new String[] {
        /* These names are provided by
         * http://oss.software.ibm.com/cgi-bin/icu/convexp?s=ALL
         */
        "ibm-1208", "ibm-1209", "ibm-5304", "ibm-5305",
        "windows-65001", "cp1208",
        // see http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
        "UTF8"
    });
  }

  public boolean contains (Charset cs)
  {
    return cs instanceof US_ASCII || cs instanceof ISO_8859_1
      || cs instanceof UTF_8 || cs instanceof UTF_16BE
      || cs instanceof UTF_16LE || cs instanceof UTF_16;
  }

  public CharsetDecoder newDecoder ()
  {
    return new Decoder (this);
  }

  public CharsetEncoder newEncoder ()
  {
    return new Encoder (this);
  }

  private static final class Decoder extends CharsetDecoder
  {
    // Package-private to avoid a trampoline constructor.
    Decoder (Charset cs)
    {
      super (cs, 1f, 1f);
    }

    protected CoderResult decodeLoop (ByteBuffer in, CharBuffer out)
    {
        return UTF8.decodeLoop(in,out);
    }
  }

  private static final class Encoder extends CharsetEncoder
  {
    // Package-private to avoid a trampoline constructor.
    Encoder (Charset cs)
    {
      // According to
      // http://www-106.ibm.com/developerworks/unicode/library/utfencodingforms/index.html
      //   On average, English takes slightly over one unit per code point.
      //   Most Latin-script languages take about 1.1 bytes. Greek, Russian,
      //   Arabic and Hebrew take about 1.7 bytes, and most others (including
      //   Japanese, Chinese, Korean and Hindi) take about 3 bytes.
      // We assume we will be dealing with latin scripts, and use 1.1 
      // for averageBytesPerChar.
      super (cs, 1.1f, 4.0f);
    }

    protected CoderResult encodeLoop (CharBuffer in, ByteBuffer out)
    {
        return UTF8.encodeLoop(in,out);
    }
  }
}
