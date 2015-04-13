/*
 * LowerHendersonFrames.java
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

import java.util.*;

public class LowerHendersonFrames extends CodePhase {
    public LowerHendersonFrames(Code c) { super(c); }

    private CLocal frame;
    
    private void emitPrologue(Header h) {
        Var framePtr=code.addVar(Exectype.POINTER);
        Var ptr=code.addVar(Exectype.POINTER);
        
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.GetCVarAddress,
                framePtr,Arg.EMPTY,
                frame));
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.GetCField,
                ptr,new Arg[]{Arg.THREAD_STATE},
                CTypesystemReferences.ThreadState_curF));
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.PutCField,
                Var.VOID,new Arg[]{
                    framePtr,
                    ptr
                },
                CTypesystemReferences.Frame_up));
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.PutCField,
                Var.VOID,new Arg[]{
                    Arg.THREAD_STATE,
                    framePtr
                },
                CTypesystemReferences.ThreadState_curF));
    }
    
    private void emitEpilogue(Header h) {
        Var framePtr=code.addVar(Exectype.POINTER);
        Var ptr=code.addVar(Exectype.POINTER);
        
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.GetCVarAddress,
                framePtr,Arg.EMPTY,
                frame));
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.GetCField,
                ptr,new Arg[]{framePtr},
                CTypesystemReferences.Frame_up));
        h.append(
            new CFieldInst(
                code.root().di(),OpCode.PutCField,
                Var.VOID,new Arg[]{
                    Arg.THREAD_STATE,
                    ptr
                },
                CTypesystemReferences.ThreadState_curF));
    }
    
    private void mogrify(Header h) {
        for (Operation o : h.operations()) {
            Var framePtr=null;
            for (int i=0;i<o.rhs().length;++i) {
                if (o.rhs(i)==Arg.FRAME) {
                    if (framePtr==null) {
                        framePtr=code.addVar(Exectype.POINTER);
                        o.prepend(
                            new CFieldInst(
                                o.di(),OpCode.GetCVarAddress,
                                framePtr,Arg.EMPTY,
                                frame));
                    }
                    o.rhs[i]=framePtr;
                }
            }
            switch (o.opcode()) {
            case SaveDebugID: {
                Var did=code.addVar(Exectype.POINTER);
                if (framePtr==null) {
                    framePtr=code.addVar(Exectype.POINTER);
                    o.prepend(
                        new CFieldInst(
                            o.di(),OpCode.GetCVarAddress,
                            framePtr,Arg.EMPTY,
                            frame));
                }
                o.prepend(
                    new DebugIDInfoInst(
                        o.di(),OpCode.GetDebugID,
                        did,
                        ((DebugIDInfoInst)o).didi()));
                o.prepend(
                    new CFieldInst(
                        o.di(),OpCode.PutCField,
                        Var.VOID,new Arg[]{
                            framePtr,
                            did
                        },
                        CTypesystemReferences.Frame_id));
                o.remove();
                break;
            }
            case SaveRef: {
                Var refArray=code.addVar(Exectype.POINTER);
                Var refPtr=code.addVar(Exectype.POINTER);
                if (framePtr==null) {
                    framePtr=code.addVar(Exectype.POINTER);
                    o.prepend(
                        new CFieldInst(
                            o.di(),OpCode.GetCVarAddress,
                            framePtr,Arg.EMPTY,
                            frame));
                }
                o.prepend(
                    new CFieldInst(
                        o.di(),OpCode.GetCFieldAddress,
                        refArray,new Arg[]{framePtr},
                        CTypesystemReferences.Frame_refs));
                o.prepend(
                    new SimpleInst(
                        o.di(),OpCode.Add,
                        refPtr,new Arg[]{
                            refArray,
                            PointerConst.make(
                                ((ArgInst)o).getIdx()*Global.pointerSize)
                        }));
                o.prepend(
                    new MemoryAccessInst(
                        o.di(),OpCode.Store,
                        Var.VOID,new Arg[]{
                            refPtr,
                            o.rhs(0)
                        },
                        o.rhs(0).type().asType()));
                o.remove();
                break;
            }
            default:
                break;
            }
        }
    }
    
    public void visitCode() {
        if (code.getLeafMethodAna().outgoingCallMode()
            == OutgoingCallMode.ARBITRARY_OUTGOING_CALLS ||
            code.getLeafMethodAna().frameUseMode()
            == FrameUseMode.FRAME_USED_ARBITRARILY) {
            
            frame=Frame.makeFrame(code.getRefAlloc().numRefs());
            code.addLocal(frame);
            
            Header root=code.reroot();
            
            if (code.getLeafMethodAna().fastPathOutgoingCallMode()
                != OutgoingCallMode.ARBITRARY_OUTGOING_CALLS &&
                code.getLeafMethodAna().frameUseMode()
                != FrameUseMode.FRAME_USED_ARBITRARILY) {
                
                // partial leaf method optimization
                
                HashSet< Header > likelyHeaders=new HashSet< Header >(code.likelyHeaders());
                
                PredsCalc pc=code.getPreds();
                
                assert code.getLeafMethodAna().outgoingCallMode()
                    == OutgoingCallMode.ARBITRARY_OUTGOING_CALLS;
                
                // 1) create landing pads for fast<->slow edges
                // 2) those landing pads will have linking code
                
                HashMap< Header, Header > fastToSlow=new HashMap< Header, Header >();
                HashMap< Header, Header > slowToFast=new HashMap< Header, Header >();
                HashSet< Header > pads=new HashSet< Header >();
                
                for (Header h : code.headers3()) {
                    // all predecessors are normal at this point
                    for (Header h2 : pc.normalPredecessors(h)) {
                        if (likelyHeaders.contains(h)!=likelyHeaders.contains(h2)) {
                            Header pad=h.makeSimilar(h.di());
                            pad.setFooter(
                                new Jump(
                                    h.di(),h));
                            pads.add(pad);
                            
                            if (likelyHeaders.contains(h)) {
                                // pretend to return
                                emitEpilogue(pad);
                                slowToFast.put(h,pad);
                            } else {
                                // pretend to enter
                                emitPrologue(pad);
                                fastToSlow.put(h,pad);
                            }
                        }
                    }
                }
                
                for (Header h : code.headers()) {
                    if (!pads.contains(h)) {
                        mogrify(h);
                        
                        if (likelyHeaders.contains(h)) {
                            h.footer().accept(
                                new MultiSuccessorReplacement(fastToSlow));
                        } else {
                            h.footer().accept(
                                new MultiSuccessorReplacement(slowToFast));
                            
                            if (EscapingFooterCalc.get(h)) {
                                emitEpilogue(h);
                            }
                        }
                    }
                }
                
            } else {
                boolean needLinking=
                    code.getLeafMethodAna().outgoingCallMode()
                    == OutgoingCallMode.ARBITRARY_OUTGOING_CALLS;

                if (needLinking) {
                    emitPrologue(root);
                }
	
                for (Header h : code.headers()) {
                    mogrify(h);

                    if (EscapingFooterCalc.get(h) && needLinking) {
                        emitEpilogue(h);
                    }
                }
            }
	
            code.killIntraBlockAnalyses();
            setChangedCode();
        } else {
            for (Header h : code.headers()) {
                for (Instruction i : h.instructions()) {
                    switch (i.opcode()) {
                    case SaveDebugID:
                    case SaveRef:
                        i.remove();
                        setChangedCode("killed SaveDebugID/SaveRef");
                        break;
                    default:
                        break;
                    }
                }
            }
            
            switch (code.getLeafMethodAna().frameUseMode()) {
            case FRAME_NOT_USED:
                break; // done!
            case FRAME_PUTCFIELD_ONLY: {
                for (Header h : code.headers()) {
                    for (Instruction i : h.instructions()) {
                        if (i.opcode()==OpCode.PutCField &&
                            i.rhs(0)==Arg.FRAME) {
                            setChangedCode("killed PutCField(FRAME)");
                            i.remove();
                        }
                    }
                }
                
                break;
            }
            default:
                throw new Error("unexpected frame use mode: "+code.getLeafMethodAna().frameUseMode());
            }
            
            if (changedCode()) {
                code.killIntraBlockAnalyses();
            }
        }
        
        // clean up
        frame=null;
    }
}

