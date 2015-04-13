/*
 * ConfigListNode.java
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

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

public final class ConfigListNode
    extends ConfigNode
    implements Iterable< ConfigNode > {

    ArrayList< ConfigNode > list=new ArrayList< ConfigNode >();
    
    public ConfigListNode() {}
    
    public ConfigListNode(Object... values) {
        appendAll(values);
    }
    
    public ConfigListNode(Collection< ? extends Object > values) {
        appendAll(values);
    }
    
    public static ConfigListNode fromArray(Object array) {
        ConfigListNode result=new ConfigListNode();
        for (int i=0;i<Array.getLength(array);++i) {
            result.append(Array.get(array,i));
        }
        return result;
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append('(');
        boolean first=true;
        for (ConfigNode cn : list) {
            if (first) {
                first=false;
            } else {
                buf.append(',');
            }
            buf.append(cn.toString());
        }
        buf.append(')');
        return buf.toString();
    }
    
    String toPrettyShortString() {
        if (isEmpty()) {
            return "()";
        }
        StringBuilder buf=new StringBuilder();
        buf.append("( ");
        boolean first=true;
        for (ConfigNode cn : list) {
            if (first) {
                first=false;
            } else {
                buf.append(", ");
            }
            buf.append(cn.toPrettyShortString());
        }
        buf.append(" )");
        return buf.toString();
    }
    
    String toPrettyMultiLineString(int startIndent,
                                   int indentLevel,
                                   int indentStep) {
        if (isEmpty()) {
            return "()";
        }
        
        boolean longForm=false;
        int maxSize;
        
        if (indentLevel<40) {
            maxSize=20;
        } else if (indentLevel<60) {
            maxSize=10;
        } else {
            maxSize=5;
        }
        
        for (ConfigNode cn : list) {
            if (cn.toPrettyShortString().length()>maxSize) {
                longForm=true;
                break;
            }
        }
        
        if (longForm) {
            StringBuilder buf=new StringBuilder();
            buf.append('(');
            buf.append(lineSeparator);
            buf.append(indent(indentLevel+indentStep));
            boolean first=true;
            for (ConfigNode cn : list) {
                if (first) {
                    first=false;
                } else {
                    buf.append(",");
                    buf.append(lineSeparator);
                    buf.append(indent(indentLevel+indentStep));
                }
                buf.append(cn.toPrettyString(indentLevel+indentStep,
                                             indentLevel+indentStep,
                                             indentStep));
            }
            buf.append(lineSeparator);
            buf.append(indent(indentLevel));
            buf.append(')');
            return buf.toString();
        } else {
            // NOTE: this may override ConfigMapNode's request to always be
            // multi-line stringified if it has more than 1 entry.  that's
            // intentional.
            StringBuilder buf=new StringBuilder();
            buf.append('(');
            buf.append(lineSeparator);
            buf.append(indent(indentLevel+indentStep));
            String str=list.get(0).toPrettyShortString();
            buf.append(str);
            int account=75-indentLevel-indentStep-str.length();
            for (int i=1;i<list.size();++i) {
                str=list.get(i).toPrettyShortString();
                if (account-str.length()-2<0) {
                    buf.append(',');
                    buf.append(lineSeparator);
                    buf.append(indent(indentLevel+indentStep));
                    buf.append(str);
                    account=75-indentLevel-indentStep-str.length();
                } else {
                    buf.append(", ");
                    buf.append(str);
                    account-=2;
                    account-=str.length();
                }
            }
            buf.append(lineSeparator);
            buf.append(indent(indentLevel));
            buf.append(')');
            return buf.toString();
        }
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public void append(ConfigNode cn) {
        if (cn==null) throw new NullPointerException();
        cn.setParent(this);
        list.add(cn);
    }
    
    public ConfigMapNode appendMap() {
        ConfigMapNode result=new ConfigMapNode();
        append(result);
        return result;
    }
    
    public ConfigListNode appendList() {
        ConfigListNode result=new ConfigListNode();
        append(result);
        return result;
    }
    
    public void append(String value) {
        append(new ConfigAtomNode(value));
    }
    
    public void append(boolean value) {
        append(new ConfigAtomNode(value));
    }
    
    public void append(int value) {
        append(new ConfigAtomNode(value));
    }
    
    public void append(long value) {
        append(new ConfigAtomNode(value));
    }
    
    public void append(double value) {
        append(new ConfigAtomNode(value));
    }
    
    public void append(Object value) {
        append(ConfigNode.toNode(value));
    }
    
    public void appendAll(Object... values) {
        for (Object value : values) {
            append(value);
        }
    }
    
    public void appendAll(Collection< ? extends Object > values) {
        for (Object value : values) {
            append(value);
        }
    }
    
    public int size() {
        return list.size();
    }
    
    public Iterator< ConfigNode > iterator() {
        return Collections.unmodifiableList(list).iterator();
    }
    
    public ConfigNode get(int i) {
        return list.get(i);
    }
    
    public ConfigAtomNode getAtom(int i) {
        return list.get(i).asAtom();
    }

    public ConfigListNode getList(int i) {
        return list.get(i).asList();
    }

    public ConfigMapNode getMap(int i) {
        return list.get(i).asMap();
    }

    public String getString(int i) {
        return getAtom(i).getString();
    }

    public boolean getBoolean(int i) {
        return getAtom(i).getBoolean();
    }

    public int getInt(int i) {
        return getAtom(i).getInt();
    }

    public long getLong(int i) {
        return getAtom(i).getLong();
    }

    public double getDouble(int i) {
        return getAtom(i).getDouble();
    }
    
    public List< ConfigNode > getNodes() {
        return Collections.unmodifiableList(list);
    }
    
    public List< String > getStrings() {
        ArrayList< String > result=new ArrayList< String >(size());
        for (int i=0;i<size();++i) {
            result.add(getString(i));
        }
        return result;
    }
    
    public boolean[] getBooleans() {
        boolean[] result=new boolean[size()];
        for (int i=0;i<result.length;++i) {
            result[i]=getBoolean(i);
        }
        return result;
    }
    
    public int[] getInts() {
        int[] result=new int[size()];
        for (int i=0;i<result.length;++i) {
            result[i]=getInt(i);
        }
        return result;
    }
    
    public long[] getLongs() {
        long[] result=new long[size()];
        for (int i=0;i<result.length;++i) {
            result[i]=getLong(i);
        }
        return result;
    }
    
    public double[] getDoubles() {
        double[] result=new double[size()];
        for (int i=0;i<result.length;++i) {
            result[i]=getDouble(i);
        }
        return result;
    }
    
    public static ConfigListNode parse(String data) {
        return ConfigNode.parse(data).asList();
    }
    
    public static ConfigListNode parse(File flnm) throws IOException {
        return ConfigNode.parse(flnm).asList();
    }
    
    // helpers
    
    String nodeTypeNameArticle() {
        return "a";
    }
    
    String nodeTypeName() {
        return "list";
    }
    
    String getContextPre(ConfigNode forNode) {
        StringBuilder buf=new StringBuilder();
        buf.append("( ");
        for (int i=0;i<size();++i) {
            if (get(i)==forNode) {
                buf.append("#");
                buf.append(i);
                buf.append(" = ");
                return buf.toString();
            }
        }
        throw new Error("bad parenting");
    }
    
    String getContextPost(ConfigNode forNode) {
        return " )";
    }
    
    String getContextInnards() {
        if (isEmpty()) {
            return "()";
        } else {
            return "( ... )";
        }
    }
}


