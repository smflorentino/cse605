/*
 * ToLIR.java
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

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

import java.util.*;

public class ToLIR {
    LCode lc;
    Code code;
    
    HashMap< CLocal, ScratchSlot > localMap=new HashMap< CLocal, ScratchSlot >();
    private HashMap< Var, Tmp > varMap=new HashMap< Var, Tmp >();
    HashMap< Header, LHeader > headMap=new HashMap< Header, LHeader >();
    
    HashSet< Var > alreadyUsed=new HashSet< Var >();
    
    BuildAddressArg baa;
    BuildLOp blo;
    
    UseCalc uc;
    SideCrossCalc scc;
    ResultTypeCalc rtc;
    
    private ScratchSlot memBounce;
    
    public ToLIR(Code code) {
        this.code=code;
        this.lc=new LCode(code.cname(),code.cresult(),code.origin());
        
        baa=new BuildAddressArg(this);
        blo=new BuildLOp(this);
        
        uc=code.getUses();
        scc=code.getSideCross();
        rtc=code.getResultType();
        
        // this thing will have some heavy bottom-up lifting to do.  here's how we
        // do it.  have a general code-generator that creates a "bottom-up" visitor.
        // the idea is simple:
        //
        // 1) a specification file describes the patterns of used-once chains (i.e.
        //    productions, in the order in which they should be detected.  an
        //    example might be:
        //
        //    loadAddStore := Store(a,Add(Load(a),b))
        //
        //    or:
        //
        //    addAdd := Add(Add(a),b)
        //
        // 2) this generates a chain detector visitor, with abstract methods like:
        //
        //    protected boolean visitLoadAddStore(Instruction store,
        //                                        Instruction add,
        //                                        Instruction load);
        //
        //    protected boolean visitAddAdd(Instruction add1,
        //                                  Instruction add2);
        //
        //    protected void visitDefault(Instruction i);
        //
        //    protected void visitIgnored(Instruction i);
        //
        // 3) the visitor provides a method like:
        //
        //    public void accept(Header h,Instruction i);
        //
        //    which considers all of the productions that may fit, in the order
        //    that they appear in the specification.  if one of the production
        //    visit methods returns true, then it marks all of the instructions
        //    that this production covered (like the Store, the Add, and the Load
        //    in loadAddStore) as "ignored".  in the future, ignored instructions
        //    result in a call to visitIgnored(), which is a no-op by default.
        //    instructions that do not match any production, or for which all
        //    matching productions return false, result in a call to visitDefault().
        
        for (CLocal cl : code.locals()) {
            localMap.put(cl, lc.addScratch((cl.sizeof()+7)&~7));
        }
        
        for (Var v : code.vars()) {
            varMap.put(v, lc.addTmp(Kind.from(v.type())));
        }
        
        for (Header h : code.headers()) {
            LHeader lh=lc.addHeader();
            lh.setFrequency(h.frequency());
            lh.setProbability(HeaderProbability.UNLIKELY_TO_EXECUTE);
            headMap.put(h, lh);
        }
        
        for (Header h : code.likelyHeaders()) {
            headMap.get(h).setProbability(HeaderProbability.DEFAULT_PROBABILITY);
        }
        
        lc.setRoot(headMap.get(code.root()));
        
        for (Header h : code.headers()) {
            LHeader lh=headMap.get(h);
            
            for (Operation o : h.reverseOperations()) {
                blo.build(lh,o);
            }
        }
    }
    
    Tmp tmpForVar(Arg a) {
        Var v=(Var)a;
        Tmp result=varMap.get(v);
        assert result!=null;
        alreadyUsed.add(v);
        return result;
    }
    
    ScratchSlot memBounce() {
        if (memBounce==null) {
            memBounce=lc.addScratch(8);
        }
        return memBounce;
    }

    CField translateField(CField f) {
        if (f instanceof JumpTable) {
            JumpTable table=(JumpTable)f;
            LHeader[] newHeaders=new LHeader[table.numHeaders()];
            for (int i=0;i<table.numHeaders();++i) {
                newHeaders[i]=headMap.get(table.header(i));
            }
            return LJumpTable.make(table.cname(),newHeaders);
        } else {
            return f;
        }
    }
    
    LType argType(Operation argOp) {
        return LType.from(code.cparam(((ArgInst)argOp).getIdx()).asExectype);
    }
    
    InParamSlot argSlot(Operation argOp,int additionalOffset) {
        // calculate the offset
        int offset=0;
        int argIdx=((ArgInst)argOp).getIdx();
        
        for (int i=0;i<argIdx;++i) {
            if (code.cparam(i).bytes<=Global.pointerSize) {
                offset+=Global.pointerSize;
            } else {
                offset+=code.cparam(i).bytes;
            }
        }
        
        return new InParamSlot(offset+additionalOffset);
    }
    
    InParamSlot argSlot(Operation argOp) {
        return argSlot(argOp,0);
    }
    
    public LCode get() {
        return lc;
    }
}

