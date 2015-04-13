/*
 * Payload.java
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

package com.fiji.mvm;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import com.fiji.fivm.util.*;
import java.util.*;

public final class Payload
    implements Comparable< Payload > {
    
    private static final boolean DEBUG=false;

    Pointer payloadPtr;
    String name;
    String entrypoint;
    
    private Payload(Pointer payloadPtr,
                    String name,
                    String entrypoint) {
        this.payloadPtr=payloadPtr;
        this.name=name;
        this.entrypoint=entrypoint;
    }
    
    public int compareTo(Payload other) {
        return name.compareTo(other.name);
    }
    
    public String getName() {
        return name;
    }
    
    public String getEntrypoint() {
        return entrypoint;
    }
    
    Pointer getSettings() {
        return payloadPtr.add(CType.offsetof("fivmr_Payload","settings"));
    }
    
    @RuntimeImport
    private static native boolean FIVMR_NOGC(Pointer settings);
    
    @RuntimeImport
    private static native boolean FIVMR_CMRGC(Pointer settings);
    
    @RuntimeImport
    private static native boolean FIVMR_HFGC(Pointer settings);
    
    public GCType getGCType() {
        if (FIVMR_NOGC(getSettings())) {
            return GCType.NOGC;
        } else if (FIVMR_CMRGC(getSettings())) {
            return GCType.CMRGC;
        } else if (FIVMR_HFGC(getSettings())) {
            return GCType.HFGC;
        } else {
            throw new fivmError("bad GC type in payload "+this);
        }
    }
    
    public int getNumInternalVMThreads() {
        return getGCType().maxNumGCThreads()+VMController.getMinNumInternalVMThreads();
    }
    
    public static final Comparator< Payload > NAME_COMPARATOR=
        new Comparator< Payload >(){
        public int compare(Payload a,Payload b) {
            return a.name.compareTo(b.name);
        }
    };
    
    public static final Comparator< Payload > ENTRYPOINT_COMPARATOR=
        new Comparator< Payload >(){
        public int compare(Payload a,Payload b) {
            return a.entrypoint.compareTo(b.entrypoint);
        }
    };
    
    private static final Comparator< Object > NAME_STRING_COMPARATOR=
        new Comparator< Object >() {
        public int compare(Object a_,Object b_) {
            CharSequence a,b;
            if (a_ instanceof CharSequence) {
                a=(CharSequence)a_;
            } else {
                a=((Payload)a_).name;
            }
            if (b_ instanceof CharSequence) {
                b=(CharSequence)b_;
            } else {
                b=((Payload)b_).name;
            }
            if (DEBUG) {
                System.out.println("a_ = "+a_+", b_ = "+b_);
                System.out.println("a = "+a+", b = "+b);
            }
            int result=CharSequenceComparator.SINGLETON.compare(a,b);
            if (DEBUG) {
                System.out.println("result = "+result);
            }
            return result;
        }
    };
    
    private static final Comparator< Object > ENTRYPOINT_STRING_COMPARATOR=
        new Comparator< Object >() {
        public int compare(Object a_,Object b_) {
            CharSequence a,b;
            if (a_ instanceof CharSequence) {
                a=(CharSequence)a_;
            } else {
                a=((Payload)a_).entrypoint;
            }
            if (b_ instanceof CharSequence) {
                b=(CharSequence)b_;
            } else {
                b=((Payload)b_).entrypoint;
            }
            return CharSequenceComparator.SINGLETON.compare(a,b);
        }
    };
    
    private static final Payload[] EMPTY_ARRAY=new Payload[0];

    private static Payload[] subPayloads;
    private static Payload[] payloadsByName;
    private static Payload[] payloadsByEntrypoint;
    private static Payload myPayload;
    
    static String className(Pointer td) {
        String typeName=
            fivmRuntime.fromCStringFull(
                fivmRuntime.fivmr_TypeData_name(td));
        if (typeName.charAt(0)!='L' ||
            typeName.charAt(typeName.length()-1)!=';') {
            throw new fivmError("Bad class name: "+typeName);
        }
        return typeName.substring(1,typeName.length()-1);
    }
    
    static {
        Pointer payloadList=
            CType.getPointer(Magic.getPayload(),
                             "fivmr_Payload",
                             "subPayloads");
        
        if (payloadList!=Pointer.zero()) {
            int nPayloads=
                CType.getPointer(payloadList,
                                 "fivmr_PayloadList",
                                 "nPayloads").castToInt();

            subPayloads=new Payload[nPayloads];
            payloadsByName=new Payload[nPayloads+1];
            payloadsByEntrypoint=new Payload[nPayloads+1];
            
            Pointer payloads=
                CType.getPointer(payloadList,
                                 "fivmr_PayloadList",
                                 "payloads");
            
            for (int i=0;i<nPayloads;++i) {
                Pointer payload=
                    payloads.add(Pointer.fromInt(i).mul(Pointer.size())).loadPointer();
                subPayloads[i]=payloadsByName[i]=payloadsByEntrypoint[i]=
                    new Payload(payload,
                                fivmRuntime.fromCStringFull(
                                    CType.getPointer(payload,
                                                     "fivmr_Payload",
                                                     "name")),
                                className(
                                    CType.getPointer(payload,
                                                     "fivmr_Payload",
                                                     "entrypoint")));
            }
        } else {
            payloadsByName=new Payload[1];
            payloadsByEntrypoint=new Payload[1];
            subPayloads=EMPTY_ARRAY;
        }
        
        myPayload=
            payloadsByName[payloadsByName.length-1]=
            payloadsByEntrypoint[payloadsByEntrypoint.length-1]=
            new Payload(Magic.getPayload(),
                        fivmRuntime.fromCStringFull(
                            CType.getPointer(Magic.getPayload(),
                                             "fivmr_Payload",
                                             "name")),
                        className(
                            CType.getPointer(Magic.getPayload(),
                                             "fivmr_Payload",
                                             "entrypoint")));
        
        Arrays.sort(payloadsByName,NAME_COMPARATOR);
        Arrays.sort(payloadsByEntrypoint,ENTRYPOINT_COMPARATOR);
        
        if (DEBUG) {
            for (int i=0;i<payloadsByName.length;++i) {
                System.out.println("payloadsByName["+i+"]: "+payloadsByName[i]);
            }
            for (int i=0;i<payloadsByEntrypoint.length;++i) {
                System.out.println("payloadsByEntrypoint["+i+"]: "+payloadsByEntrypoint[i]);
            }
        }
    }
    
    private static Payload getPayloadImpl(CharSequence name,
                                          Payload[] payloadList,
                                          Comparator< Object > comparator) {
        int index=Arrays.binarySearch(payloadList,
                                      name,
                                      comparator);
        if (DEBUG) {
            System.out.println("index = "+index);
        }
        if (index<0) {
            return null;
        } else {
            if (DEBUG) {
                System.out.println("returning: "+payloadList[index]);
            }
            return payloadList[index];
        }
    }
    
    public static Payload getPayloadByEntrypoint(CharSequence name) {
        return getPayloadImpl(name,payloadsByEntrypoint,ENTRYPOINT_STRING_COMPARATOR);
    }
    
    public static Payload getPayloadByName(CharSequence name) {
        return getPayloadImpl(name,payloadsByName,NAME_STRING_COMPARATOR);
    }
    
    public static Payload getPayload(CharSequence name) {
        return getPayloadByName(name);
    }
    
    public static Payload getMyPayload() {
        return myPayload;
    }
    
    private static List< Payload > PAYLOADS=
        Collections.unmodifiableList(Arrays.asList(payloadsByName));
    
    public static List< Payload > payloads() {
        return PAYLOADS;
    }
    
    private static List< Payload > SUB_PAYLOADS=
        Collections.unmodifiableList(Arrays.asList(subPayloads));
    
    public static List< Payload > subPayloads() {
        return SUB_PAYLOADS;
    }
    
    public String toString() {
        return "Payload[Name = "+name+", Entrypoint = "+entrypoint+"]";
    }
}

