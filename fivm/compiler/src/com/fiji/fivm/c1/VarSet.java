/*
 * VarSet.java
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

import com.fiji.fivm.IntUtil;
import java.util.*;

public class VarSet implements Iterable< Var > {
    Code code;
    int nbits;
    int[] vec;
    
    VarSet(int[] vec,Code code,int nbits) {
        this.vec=vec;
        this.code=code;
        this.nbits=nbits;
    }
    
    public VarSet(Code c) {
        code=c;
        nbits=c.vars().size();
        vec=IntUtil.newBitSet(nbits);
    }
    
    public VarSet(VarSet vs) {
        this(vs.code);
        addAll(vs);
    }
    
    public boolean add(Var v) {
        return IntUtil.setBit(vec,v.id);
    }
    
    public boolean remove(Var v) {
        if (v.id>=0) {
            return IntUtil.clrBit(vec,v.id);
        } else {
            return false;
        }
    }
    
    public boolean set(Var v,boolean value) {
        if (value) {
            return add(v);
        } else {
            return remove(v);
        }
    }
    
    public boolean remove(Object o) {
        if (o instanceof Var) {
            return remove((Var)o);
        } else {
            return false;
        }
    }
    
    public boolean addAll(VarSet vs) {
        return IntUtil.or(vec,vs.vec);
    }
    
    public boolean addAll(Iterable< Var > vs) {
        boolean changed=false;
        for (Var v : vs) {
            changed|=add(v);
        }
        return changed;
    }
    
    public boolean retainAll(VarSet vs) {
        return IntUtil.and(vec,vs.vec);
    }
    
    public boolean removeAll(VarSet vs) {
        return IntUtil.andNot(vec,vs.vec);
    }
    
    public boolean removeAll(Iterable< Var > vs) {
        boolean changed=false;
        for (Var v : vs) {
            changed|=remove(v);
        }
        return changed;
    }
    
    public boolean contains(Var v) {
        if (v.id>=0) {
            return IntUtil.bit(vec,v.id);
        } else {
            return false;
        }
    }
    
    public boolean contains(Object o) {
        if (o instanceof Var) {
            return contains((Var)o);
        } else {
            return false;
        }
    }
    
    public boolean get(Var v) {
        return contains(v);
    }
    
    public boolean get(Object o) {
        return contains(o);
    }
    
    public void cascade(VarSet vs1,VarSet vs2) {
        IntUtil.cascade(vec,vs1.vec,vs2.vec);
    }
    
    public void clear() {
        IntUtil.clear(vec);
    }
    
    public VarSet copy() {
        return new VarSet(IntUtil.copy(vec),code,nbits);
    }
    
    public Iterator< Var > iterator() {
        return new Iterator< Var >(){
            int idx=IntUtil.nextSetBit(vec,0,nbits);
            int cnt=0;
            public boolean hasNext() {
                if (idx<nbits) {
                    return true;
                } else {
                    return false;
                }
            }
            public Var next() {
                if (idx>=nbits) {
                    throw new NoSuchElementException();
                }
                Var result=code.vars().get(idx);
                idx=IntUtil.nextSetBit(vec,idx+1,nbits);
                cnt++;
                return result;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public int cardinality() {
        return IntUtil.cardinality(vec,nbits);
    }
    
    public boolean isEmpty() {
        return IntUtil.isEmpty(vec,nbits);
    }
    
    public boolean equals(Object other_) {
        if (!(other_ instanceof VarSet)) return false;
        VarSet other=(VarSet)other_;
        return code==other.code
            && nbits==other.nbits
            && IntUtil.setsEqual(vec,other.vec);
    }
    
    public int hashCode() {
        return IntUtil.setHashCode(vec,nbits);
    }
    
    public String toString() {
        StringBuilder result=new StringBuilder();
        result.append("VarSet[");
        boolean first=true;
        for (Var v : this) {
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


