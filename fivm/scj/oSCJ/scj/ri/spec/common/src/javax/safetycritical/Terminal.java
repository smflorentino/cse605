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

/**
 * 
 */
package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class Terminal {

    private static Terminal single = new Terminal();

    private Terminal() {
    }

    /**
     * Get the single output device.
     * 
     * @return something
     */
    @SCJAllowed
    public static Terminal getTerminal() {
        return single;
    }

    /**
     * Write the character sequence to the implementation dependent output
     * device in UTF8.
     * 
     * @param s
     * 
     */
    @SCJAllowed
    public void write(CharSequence s) {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c < 128) {
                write((byte) (c & 0x7f));
            } else if (c < 0x800) {
                write((byte) (0xc0 | (c >>> 6)));
                write((byte) (0x80 | (c & 0x3f)));
            } else if (c < 0x1000) {
                write((byte) (0xe0 | (c >>> 12)));
                write((byte) (0x80 | ((c >>> 6) & 0x3f)));
                write((byte) (0x80 | (c & 0x3f)));
            } else {
                // TODO: we don't care on unicode that needs an escape itself
            }
        }
    }

    /**
     * Same as write, but add a newline. CRLF does not hurt on a Unix terminal.
     * 
     * @param s
     */
    @SCJAllowed
    public void writeln(CharSequence s) {
        write(s);
        writeln();
    }

    /**
     * Just a CRLF output.
     */
    @SCJAllowed
    public void writeln() {
        write("\r\n");
    }

    /**
     * Does the actual work. TODO: Change for your implementation.
     * 
     * @param b
     *            A UTF8 byte to be written.
     */
    private void write(byte b) {
        java.lang.System.out.write(b);
    }
}
