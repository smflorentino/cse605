/*
 * ResolutionID.java
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

import com.fiji.config.*;

public final class ResolutionID {
    private ClassResolutionMode resMode=ClassResolutionMode.UNRESOLVED;
    private String context;
    private String klass;
    private String member;
    
    public ResolutionID(ClassResolutionMode resMode,
                        String context,
                        String klass,
                        String member) {
        this.resMode=resMode;
        this.context=context;
        this.klass=klass;
        this.member=member;
    }
    
    public ResolutionID(ClassResolutionMode resMode,
                        String context,
                        String klass) {
        this(resMode,context,klass,null);
    }
    
    public ResolutionID(ResolutionID other) {
        this.resMode=other.resMode;
        this.context=other.context;
        this.klass=other.klass;
        this.member=other.member;
    }
    
    public ResolutionID(ResolutionID klass,
                        String member) {
        this.resMode=klass.resMode;
        this.context=klass.context;
        this.klass=klass.klass;
        this.member=member;
    }
    
    public ResolutionID(VisibleClass klass) {
        this(klass.getResolutionID());
    }
    
    public ResolutionID(VisibleClass klass,
                        String member) {
        this(klass.getResolutionID(),member);
    }
    
    public ResolutionID(ConfigNode node) {
        ConfigMapNode map=node.asMap();
        this.context=map.getString("context");
        this.klass=map.getString("class");
        if (map.has("member")) {
            this.member=map.getString("member");
        }
    }
    
    public ClassResolutionMode getClassResolutionMode() {
        return resMode;
    }
    
    public String getContext() {
        return context;
    }
    
    public String getKlass() {
        return klass;
    }
    
    public boolean hasMember() {
        return member!=null;
    }
    
    public String getMember() {
        return member;
    }
    
    public void resolutionCanceled() {
        resMode=ClassResolutionMode.RESOLUTION_CANCELED;
    }
    
    public ConfigNode asConfigNode() {
        ConfigMapNode result=
            new ConfigMapNode("resMode",resMode.toString(),
                              "context",context,
                              "class",klass);
        if (member!=null) {
            result.put("member",member);
        }
        return result;
    }
    
    public int hashCode() {
        int result=context.hashCode()+klass.hashCode();
        if (member!=null) {
            result+=member.hashCode();
        }
        return result;
    }
    
    public boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof ResolutionID)) return false;
        ResolutionID other=(ResolutionID)other_;
        return context.equals(other.context)
            && klass.equals(other.klass)
            && (member==null)==(other.member==null)
            && (member==null || member.equals(other.member));
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append(context);
        buf.append(":L");
        buf.append(klass);
        buf.append(";");
        if (member!=null) {
            buf.append("/");
            buf.append(member);
        }
        return buf.toString();
    }
    
    public String forReport() {
        StringBuilder buf=new StringBuilder();
        if (resMode==ClassResolutionMode.RESOLVED) {
            buf.append(context);
            buf.append(":");
        }
        buf.append("L");
        buf.append(klass);
        buf.append(";");
        if (member!=null) {
            buf.append("/");
            buf.append(member);
        }
        return buf.toString();
    }
}

