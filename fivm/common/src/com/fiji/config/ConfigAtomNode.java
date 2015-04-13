/*
 * ConfigAtomNode.java
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

package com.fiji.config;

import java.io.*;

public final class ConfigAtomNode extends ConfigNode {
    String value;
    boolean identifier;
    
    public ConfigAtomNode(String value) {
        if (value==null) throw new NullPointerException();
        this.value=value;
        checkIdentifier();
    }
    
    public ConfigAtomNode(CharSequence value) {
        if (value==null) throw new NullPointerException();
        StringBuilder buf=new StringBuilder();
        buf.append(value);
        this.value=buf.toString();
        checkIdentifier();
    }
    
    public ConfigAtomNode(boolean value) {
        if (value) {
            this.value="yes";
        } else {
            this.value="no";
        }
        identifier=true;
    }
    
    public ConfigAtomNode(long value) {
        this.value=""+value;
        checkIdentifier();
    }
    
    public ConfigAtomNode(int value) {
        this.value=""+value;
        checkIdentifier();
    }
    
    public ConfigAtomNode(double value) {
        this.value=""+value;
        checkIdentifier();
    }
    
    public ConfigAtomNode(Object value) {
        if (value instanceof Number) {
            this.value=""+value;
        } else if (value instanceof CharSequence) {
            StringBuilder buf=new StringBuilder();
            buf.append((CharSequence)value);
            this.value=buf.toString();
        } else if (value instanceof Boolean) {
            boolean value_=(Boolean)value;
            if (value_) {
                this.value="yes";
            } else {
                this.value="no";
            }
        } else {
            throw new IllegalArgumentException("Cannot create atom from "+value+"; "+
                                               value.getClass()+" is not an acceptable type");
        }
        checkIdentifier();
    }
    
    public String toString() {
        if (identifier) {
            return value;
        } else {
            return toQuotedString();
        }
    }
    
    String toPrettyShortString() {
        return toString();
    }
    
    String toPrettyMultiLineString(int startIndent,
                                   int indentLevel,
                                   int indentStep) {
        return toString();
    }
    
    public static String toQuotedString(String value) {
        try {
            StringBuilder buf=new StringBuilder();
            buf.append('\"');
            byte[] bytes=value.getBytes("UTF-8");
            for (byte b : bytes) {
                char c=(char)b;
                if (isalnum(c)) {
                    buf.append(c);
                } else {
                    switch (c) {
                    case ' ':
                    case '~':
                    case '!':
                    case '#':
                    case '$':
                    case '%':
                    case '^':
                    case '&':
                    case '*':
                    case '(':
                    case ')':
                    case '-':
                    case '_':
                    case '+':
                    case '=':
                    case '{':
                    case '}':
                    case '[':
                    case ']':
                    case '|':
                    case ':':
                    case ';':
                    case '<':
                    case '>':
                    case ',':
                    case '.':
                    case '?':
                    case '/':
                        buf.append(c);
                        break;
                    case '\"':
                    case '\'':
                        buf.append('\\');
                        buf.append(c);
                        break;
                    case '\r':
                        buf.append("\\r");
                        break;
                    case '\n':
                        buf.append("\\n");
                        break;
                    case '\t':
                        buf.append("\\t");
                        break;
                    default:
                        buf.append('\\');
                        buf.append(toOct((c>>6)&3));
                        buf.append(toOct((c>>3)&7));
                        buf.append(toOct((c>>0)&7));
                        break;
                    }
                }
            }
            buf.append('\"');
            return buf.toString();
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }        
    
    public String toQuotedString() {
        return toQuotedString(value);
    }
    
    public String get() {
        return value;
    }
    
    public String getString() {
        return value;
    }
    
    public boolean getBoolean() {
        if (value.equals("true") ||
            value.equals("yes") ||
            value.equals("True") ||
            value.equals("Yes") ||
            value.equals("TRUE") ||
            value.equals("YES") ||
            value.equals("1")) {
            return true;
        } else if (value.equals("false") ||
                   value.equals("no") ||
                   value.equals("False") ||
                   value.equals("No") ||
                   value.equals("FALSE") ||
                   value.equals("NO") ||
                   value.equals("0")) {
            return false;
        } else {
            throw new StringConversionException(
                "Could not convert "+getContextDescription()+" to a boolean");
        }
    }
    
    public int getInt() {
        int result=0;
        try {
            result=Integer.parseInt(value);
        } catch (NumberFormatException e) {}
        if (!(""+result).equals(value)) {
            throw new StringConversionException(
                "Could not convert "+getContextDescription()+" to an integer");
        }
        return result;
    }
    
    public long getLong() {
        long result=0;
        try {
            result=Long.parseLong(value);
        } catch (NumberFormatException e) {}
        if (!(""+result).equals(value)) {
            throw new StringConversionException(
                "Could not convert "+getContextDescription()+" to a long integer");
        }
        return result;
    }
    
    public double getDouble() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new StringConversionException(
                "Could not convert "+getContextDescription()+" to a "+
                "double-precision floating point number");
        }
    }
    
    public static ConfigAtomNode parse(String data) {
        return ConfigNode.parse(data).asAtom();
    }
    
    public static ConfigAtomNode parse(File flnm) throws IOException {
        return ConfigNode.parse(flnm).asAtom();
    }
    
    // helpers
    
    void checkIdentifier() {
        if (value.length()==0) {
            identifier=false;
            return;
        }
        for (int i=0;i<value.length();++i) {
            char c=value.charAt(i);
            if (!isalnum(c) &&
                c != '_' &&
                c != '$' &&
                c != '-' &&
                c != '/' &&
                c != '.' &&
                c != '+') {
                identifier=false;
                return;
            }
        }
        identifier=true;
    }
    
    String nodeTypeNameArticle() {
        return "an";
    }
    
    String nodeTypeName() {
        return "atom";
    }
    
    String getContextInnards() {
        if (isContextRoot()) {
            return toQuotedString();
        } else {
            return toString();
        }
    }
}


