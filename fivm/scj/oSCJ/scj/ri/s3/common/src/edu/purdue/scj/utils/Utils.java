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

package edu.purdue.scj.utils;

public final class Utils {

    /**
     * debugging flag
     */
    public static boolean DEBUG = true;

    private static int indent;
    
    // add prefix to your debug prints here, eg "###[SCJ debug]:"
    private static final String prefix = "";
    
    private static String[] indentSpace = {"",
                                      " ",
                                      "  ",
                                      "   ",
                                      "    ",
                                      "     ",
                                      "      ",
                                      "       ",
                                      "        ",
                                      "         ",
                                      "          ",
                                      "           ", //12
                                      "            "};
    
   

    
    public static void increaseIndent() {
        indent++;
    }

    public static void decreaseIndent() {
        
        if (indent > 0)
            indent--;    
        
        if (!exit.equals("")) {
            debugPrintln(exit + " : exit.");
            exit = "";
        }
    }
    
    public static void debugPrint(String msg) {
        if (DEBUG) {
            if (indent < indentSpace.length)
                System.err.print(indentSpace[indent] + prefix + msg);
            else
                System.err.print(indentSpace[indentSpace.length-1] + prefix + msg);
        }
    }

    public static void debugPrintln(String msg) {
        if (DEBUG) {
            if (indent < indentSpace.length)
                System.err.println(indentSpace[indent] + prefix + msg);
            else
                System.err.println(indentSpace[indentSpace.length-1] + prefix + msg);
        }
    }

    public static void panic(String msg) {
        System.out.println(msg);
    }

    private static String exit = "";
    
    public static void debugIndentIncrement(String string) {
        increaseIndent();
        debugPrintln(string);
        exit = string;
    }
}
