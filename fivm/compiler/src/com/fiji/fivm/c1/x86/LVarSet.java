/*
 * LVarSet.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.IntUtil;
import com.fiji.fivm.c1.x86.arg.LArg;
import com.fiji.fivm.c1.x86.arg.Tmp;

import java.util.*;

public class LVarSet implements Iterable< LArg > {
    LCode code;
    int nbits;
    int[] vec;
    
    LVarSet(int[] vec,LCode code,int nbits) {
        this.vec=vec;
        this.code=code;
        this.nbits=nbits;
    }
    
    public LVarSet(LCode c) {
        code=c;
        nbits=c.numVars();
        vec=IntUtil.newBitSet(nbits);
    }
    
    public LVarSet(LVarSet vs) {
        this(vs.code);
        addAll(vs);
    }
    
    public LVarSet(LCode c,LArg[] args) {
        this(c);
        for (LArg a : args) {
            add(a);
        }
    }
    
    public boolean add(LArg v) {
        return IntUtil.setBit(vec,v.id());
    }
    
    public boolean remove(LArg v) {
        if (v.variable()) {
            return IntUtil.clrBit(vec,v.id());
        } else {
            return false;
        }
    }
    
    public boolean set(LArg v,boolean value) {
        if (value) {
            return add(v);
        } else {
            return remove(v);
        }
    }
    
    public boolean remove(Object o) {
        if (o instanceof LArg) {
            return remove((LArg)o);
        } else {
            return false;
        }
    }
    
    public boolean addAll(LVarSet vs) {
        return IntUtil.or(vec,vs.vec);
    }
    
    public boolean addAll(Iterable< ? extends LArg > vs) {
        boolean changed=false;
        for (LArg v : vs) {
            changed|=add(v);
        }
        return changed;
    }
    
    public boolean removeAll(LVarSet vs) {
        return IntUtil.andNot(vec,vs.vec);
    }
    
    public boolean removeAll(Iterable< ? > vs) {
        boolean changed=false;
        for (Object v : vs) {
            changed|=remove(v);
        }
        return changed;
    }
    
    public boolean contains(LArg v) {
        if (v.variable()) {
            return IntUtil.bit(vec,v.id());
        } else {
            return false;
        }
    }
    
    public boolean contains(Object o) {
        if (o instanceof LArg) {
            return contains((LArg)o);
        } else {
            return false;
        }
    }
    
    public boolean get(LArg v) {
        return contains(v);
    }
    
    public boolean get(Object o) {
        return contains(o);
    }
    
    public void cascade(LVarSet vs1,LVarSet vs2) {
        IntUtil.cascade(vec,vs1.vec,vs2.vec);
    }
    
    public void clear() {
        IntUtil.clear(vec);
    }
    
    public LVarSet copy() {
        return new LVarSet(IntUtil.copy(vec),code,nbits);
    }
    
    public Iterator< LArg > iterator() {
        return new Iterator< LArg >(){
            int idx=IntUtil.nextSetBit(vec,0,nbits);
            int cnt=0;
            public boolean hasNext() {
                if (idx<nbits) {
                    return true;
                } else {
                    return false;
                }
            }
            public LArg next() {
                if (idx>=nbits) {
                    throw new NoSuchElementException();
                }
                LArg result=code.getVar(idx);
                idx=IntUtil.nextSetBit(vec,idx+1,nbits);
                cnt++;
                return result;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public HashSet< Tmp > toTmpSet() {
        HashSet< Tmp > result=new HashSet< Tmp >();
        for (LArg a : this) {
            if (a instanceof Tmp) {
                result.add((Tmp)a);
            }
        }
        return result;
    }
    
    public int cardinality() {
        return IntUtil.cardinality(vec,nbits);
    }
    
    public boolean isEmpty() {
        return IntUtil.isEmpty(vec,nbits);
    }
    
    public boolean equals(Object other_) {
        if (!(other_ instanceof LVarSet)) return false;
        LVarSet other=(LVarSet)other_;
        return code==other.code
            && nbits==other.nbits
            && IntUtil.setsEqual(vec,other.vec);
    }
    
    public int hashCode() {
        return IntUtil.setHashCode(vec,nbits);
    }
    
    public String toString() {
        StringBuilder result=new StringBuilder();
        result.append("LVarSet[");
        boolean first=true;
        for (LArg v : this) {
            if (first) {
                first=false;
            } else {
                result.append(", ");
            }
            result.append(v);
        }
        if (true) {
            result.append(", ");
            for (int i=0;i<nbits;++i) {
                if (IntUtil.bit(vec,i)) {
                    result.append("1");
                } else {
                    result.append("0");
                }
            }
        }
        result.append("]");
        return result.toString();
    }
}


