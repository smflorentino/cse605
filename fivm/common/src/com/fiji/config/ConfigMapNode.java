/*
 * ConfigMapNode.java
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

public final class ConfigMapNode extends ConfigNode {
    // FIXME: the TreeMap should be sorted in such a way that
    // strings that contain numbers appear sorted, and so that numbers
    // always come before everything else.  or... we could just make this
    // a LinkedHashMap.  ah, who knows.
    
    TreeMap< String, ConfigNode > map=new TreeMap< String, ConfigNode >();
    
    public ConfigMapNode() {}
    
    public ConfigMapNode(Object... values) {
        putAll(values);
    }
    
    public ConfigMapNode(Map< String, ? extends Object > map) {
        putAll(map);
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    public int size() {
        return map.size();
    }
    
    public void put(String key,
                    ConfigNode value) {
        if (key==null || value==null) {
            throw new NullPointerException();
        }
        value.setParent(this);
        map.put(key,value);
    }
    
    public ConfigMapNode putMap(String key) {
        ConfigMapNode result=new ConfigMapNode();
        put(key,result);
        return result;
    }
    
    public ConfigListNode putList(String key) {
        ConfigListNode result=new ConfigListNode();
        put(key,result);
        return result;
    }

    public void put(Object key,
                    String value) {
        put(key.toString(),new ConfigAtomNode(value));
    }
    
    public void put(Object key,
                    boolean value) {
        put(key.toString(),new ConfigAtomNode(value));
    }
    
    public void put(Object key,
                    int value) {
        put(key.toString(),new ConfigAtomNode(value));
    }
    
    public void put(Object key,
                    long value) {
        put(key.toString(),new ConfigAtomNode(value));
    }
    
    public void put(Object key,
                    double value) {
        put(key.toString(),new ConfigAtomNode(value));
    }
    
    public void put(Object key,
                    Object value) {
        put(key.toString(),ConfigNode.toNode(value));
    }
    
    public void putAll(Object... values) {
        if ((values.length&1)!=0) {
            throw new IllegalArgumentException(
                "The ConfigMapNode(Object... values) constructor and putAll(Object... values) "+
                "method requires an even number of arguments, which correspond to key-value "+
                "pairs.");
        }
        
        for (int i=0;i<values.length;i+=2) {
            String key=values[i+0].toString();
            try {
                put(key,values[i+1]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid value at argument "+(i+1+1)+": "+values[i+1]+"; "+
                    values[i+1].getClass()+" is not an acceptable type");
            }
        }
    }
    
    public void putAll(Map< ? extends Object, ? extends Object > map) {
        for (Map.Entry< ? extends Object, ? extends Object > e : map.entrySet()) {
            put(e.getKey(),e.getValue());
        }
    }
    
    public void remove(Object key) {
        map.remove(key.toString());
    }
    
    public boolean has(Object key) {
        return map.containsKey(key.toString());
    }
    
    public ConfigNode get(Object key) {
        return map.get(key.toString());
    }
    
    public ConfigNode getNode(Object key) {
        ConfigNode result=get(key);
        if (result==null) {
            throw new NodeNotFoundException(
                "Could not find key "+ConfigAtomNode.toQuotedString(key.toString())+" in "+
                getContextDescription());
        }
        return result;
    }
    
    public ConfigAtomNode getAtom(Object key) {
        return getNode(key).asAtom();
    }
    
    public ConfigListNode getList(Object key) {
        return getNode(key).asList();
    }
    
    public ConfigMapNode getMap(Object key) {
        return getNode(key).asMap();
    }
    
    public String getString(Object key) {
        return getAtom(key).getString();
    }
    
    public String getString(Object key,
                            String def) {
        ConfigNode n=get(key);
        if (n==null) {
            return def;
        } else {
            return n.asAtom().getString();
        }
    }
    
    public boolean getBoolean(Object key) {
        return getAtom(key).getBoolean();
    }
    
    public boolean getBoolean(Object key,
                              boolean def) {
        ConfigNode n=get(key);
        if (n==null) {
            return def;
        } else {
            return n.asAtom().getBoolean();
        }
    }
    
    public int getInt(Object key) {
        return getAtom(key).getInt();
    }
    
    public int getInt(Object key,
                      int def) {
        ConfigNode n=get(key);
        if (n==null) {
            return def;
        } else {
            return n.asAtom().getInt();
        }
    }
    
    public long getLong(Object key) {
        return getAtom(key).getLong();
    }

    public long getLong(Object key,
                        long def) {
        ConfigNode n=get(key);
        if (n==null) {
            return def;
        } else {
            return n.asAtom().getLong();
        }
    }
    
    public double getDouble(Object key) {
        return getAtom(key).getDouble();
    }

    public double getDouble(Object key,
                            double def) {
        ConfigNode n=get(key);
        if (n==null) {
            return def;
        } else {
            return n.asAtom().getDouble();
        }
    }
    
    public Set< String > keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    public Collection< ConfigNode > values() {
        return Collections.unmodifiableCollection(map.values());
    }
    
    public Set< Map.Entry< String, ConfigNode > > entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }
    
    public Map< String, ConfigNode > map() {
        return Collections.unmodifiableMap(map);
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append('{');
        for (Map.Entry< String, ConfigNode > e : entrySet()) {
            buf.append(new ConfigAtomNode(e.getKey()).toString());
            buf.append('=');
            buf.append(e.getValue().toString());
            buf.append(';');
        }
        buf.append('}');
        return buf.toString();
    }
    
    public static ConfigMapNode parse(String data) {
        return ConfigNode.parse(data).asMap();
    }
    
    public static ConfigMapNode parse(File flnm) throws IOException {
        return ConfigNode.parse(flnm).asMap();
    }
    
    // helpers
    
    String toPrettyShortString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buf=new StringBuilder();
        buf.append("{ ");
        for (Map.Entry< String, ConfigNode > e : entrySet()) {
            buf.append(new ConfigAtomNode(e.getKey()).toString());
            buf.append(" = ");
            buf.append(e.getValue().toPrettyShortString());
            buf.append("; ");
        }
        buf.append('}');
        return buf.toString();
    }
    
    String toPrettyMultiLineString(int startIndent,
                                   int indentLevel,
                                   int indentStep) {
        if (isEmpty()) {
            return "{}";
        }
        
        StringBuilder buf=new StringBuilder();
        buf.append('{');
        buf.append(lineSeparator);
        buf.append(indent(indentLevel));
        for (Map.Entry< String, ConfigNode > e : entrySet()) {
            buf.append(indent(indentStep));
            String key=new ConfigAtomNode(e.getKey()).toString();
            buf.append(key);
            buf.append(" = ");
            buf.append(e.getValue().toPrettyString(indentLevel+indentStep+key.length()+3,
                                                   indentLevel+indentStep,
                                                   indentStep));
            buf.append(';');
            buf.append(lineSeparator);
            buf.append(indent(indentLevel));
        }
        buf.append('}');
        return buf.toString();
    }
    
    boolean forceMultiLine() {
        return size()>1;
    }
    
    String getContextPre(ConfigNode forNode) {
        StringBuilder buf=new StringBuilder();
        buf.append("{ ");
        for (Map.Entry< String, ConfigNode > e : entrySet()) {
            if (e.getValue()==forNode) {
                buf.append(new ConfigAtomNode(e.getKey()).toString());
                buf.append(" = ");
                return buf.toString();
            }
        }
        throw new Error("bad parenting");
    }
    
    String getContextPost(ConfigNode forNode) {
        return " }";
    }
    
    String getContextInnards() {
        if (isEmpty()) {
            return "{}";
        } else {
            return "{ ... }";
        }
    }
    
    String nodeTypeNameArticle() {
        return "a";
    }
    
    String nodeTypeName() {
        return "map";
    }
}


