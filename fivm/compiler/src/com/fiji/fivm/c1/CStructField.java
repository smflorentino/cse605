/*
 * CStructField.java
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

/**
 * Represents a field in a C structure.  Indicates the field's name (name), the name of the C type corresponding to the struct (structName),
 * and the type of the field using the Basetype type system (type).  The type is only needed if the field will be used for GetCField
 * or PutCField instructions.  Typically, fields whose types cannot be expressed using the Basetype type system are marked Basetype.VOID,
 * and are only accessed by getting their offset (GetCFieldOffset, GetCFieldAddress).
 * 
 * @author Filip Pizlo
 */
public class CStructField extends CField {
    
    CType struct;
    int offsetof=-1;
    String from;
    
    private CStructField(boolean typeUnknown,Basetype type,String name,CType struct,Object from) {
	super(typeUnknown,type,name);
	this.struct=struct;
        this.typeUnknown=typeUnknown;
        this.from=from.toString();
    }
    
    public static CStructField make(Basetype type, String name,CType struct,Object from) {
	return CTypesystemReferences.addField(
            new CStructField(false, type, name, struct, from));
    }
    
    public static CStructField make(Basetype type, String name, String structName,Object from) {
	return make(type,name,CType.forName(structName),from);
    }

    public static CStructField make(String name,CType struct,Object from) {
	return CTypesystemReferences.addField(
            new CStructField(true, Basetype.VOID, name, struct, from));
    }
    
    public static CStructField make(String name, String structName,Object from) {
	return make(name,CType.forName(structName),from);
    }
    
    public static CStructField make(Basetype type, String name, CType struct) {
        return make(type,name,struct,CStructField.class);
    }
    
    public static CStructField make(Basetype type, String name, String structName) {
        return make(type,name,structName,CStructField.class);
    }
    
    public static CStructField make(String name, CType struct) {
        return make(name,struct,CStructField.class);
    }
    
    public static CStructField make(String name, String structName) {
        return make(name,structName,CStructField.class);
    }

    public String getStructName() { return struct.asCCode(); }
    
    public CType getStruct() { return struct; }
    
    public int offsetofImpl() {
        return offsetof;
    }
    
    public int offsetof() {
        int result=offsetofImpl();
        assert result>=0 : this;
        return result;
    }
    
    public int hashCode() {
	return name.hashCode()+struct.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof CStructField)) return false;
	CStructField other=(CStructField)other_;
        return name.equals(other.name)
            && struct.equals(other.struct);
    }
    
    public String toString() {
	return "CStructField["+type+" "+struct.name()+"::"+name+"]";
    }
    
}
