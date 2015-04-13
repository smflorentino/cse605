/*
 * Main.java
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

package com.fiji.fivm.bottomup;

import java.util.*;

public class Main {
    static LinkedHashMap< String, Production > prods;
    static int tmpCnt;
    static int maxOpVars;
    
    static String newVar() {
        return "__tmp"+(++tmpCnt);
    }
    
    static abstract class Continuation {
        abstract void call(ArrayList< String > opVars,
                           LinkedHashMap< String, Production > ps);
    }
    
    static LinkedHashSet< String > collectHeads(LinkedHashMap< String, Production > ps) {
        LinkedHashSet< String > result=new LinkedHashSet< String >();
        for (Production p : ps.values()) {
            result.add(p.name());
        }
        return result;
    }
    
    static LinkedHashMap< String, Production > prune(LinkedHashMap< String, Production > ps,
                                                     String head) {
        LinkedHashMap< String, Production > result=new LinkedHashMap< String, Production >();
        for (Map.Entry< String, Production > e : ps.entrySet()) {
            if (e.getValue().name().equals(head)) {
                result.put(e.getKey(),e.getValue());
            }
        }
        return result;
    }
    
    static LinkedHashSet< String > collectHeadsOnArg(LinkedHashMap< String, Production > ps,
                                                     int argIdx) {
        LinkedHashSet< String > result=new LinkedHashSet< String >();
        for (Production p : ps.values()) {
            if (p.args().get(argIdx).isOperation()) {
                result.add(p.args().get(argIdx).name());
            }
        }
        return result;
    }
    
    /** Get a map of top-level production names, to inner productions at the argument index
        argIdx that all correspond to the operation called head. */
    static LinkedHashMap< String, Production > pruneOpOnArg(LinkedHashMap< String, Production > ps,
                                                            int argIdx,
                                                            String head) {
        LinkedHashMap< String, Production > result=new LinkedHashMap< String, Production >();
        for (Map.Entry< String, Production > e : ps.entrySet()) {
            if (e.getValue().args().get(argIdx).isOperation() &&
                e.getValue().args().get(argIdx).name().equals(head)) {
                result.put(e.getKey(),e.getValue().args().get(argIdx));
            }
        }
        return result;
    }
    
    static void justTryProduction(ArrayList< String > opVars,String key) {
        System.out.println("this.numOpVars=numOpVars+"+(opVars.size()-1)+";");

        Production p=prods.get(key);
        ArrayList< String > opVarNames=p.operationVarNames();
        
        HashMap< String, Integer > varIndices=new HashMap< String, Integer >();
        for (int i=0;i<opVarNames.size();++i) {
            varIndices.put(opVarNames.get(i),i);
        }
        
        HashMap< String, ArrayList< RhsReference > > requiredEq=p.requiredEqualities();
        
        for (ArrayList< RhsReference > eqSet : requiredEq.values()) {
            try {
                String leftEncVar=opVars.get(varIndices.get(eqSet.get(0).enclosing().varName()));
                
                String leftVar=newVar();
                
                System.out.println("Arg "+leftVar+"="+leftEncVar+".rhs("+eqSet.get(0).index()+");");
                System.out.print("if (");
                
                for (int i=1;i<eqSet.size();++i) {
                    if (i>1) {
                        System.out.println(" &&");
                    }
                    System.out.print(
                        "argsEqual("+leftVar+","+
                        opVars.get(varIndices.get(eqSet.get(i).enclosing().varName()))+".rhs("+
                        eqSet.get(i).index()+"))");
                }
                
                System.out.println(") {");
            } catch (Throwable e) {
                throw new Error(
                    "Failed on eqSet = "+eqSet+", opVars = "+opVars+", opVarNames = "+opVarNames+", varIndices = "+varIndices+", requiredEq = "+requiredEq,e);
            }
        }
        
        HashSet< String > requiredImSeen=new HashSet< String >();
        ArrayList< RhsReference > requiredIm=p.requiredImmediates();
        
        if (!requiredIm.isEmpty()) {
            System.out.print("if (");
            boolean first=true;
            for (RhsReference ref : requiredIm) {
                if (requiredImSeen.add(ref.production().name())) {
                    if (first) {
                        first=false;
                    } else {
                        System.out.println(" &&");
                    }
                    System.out.print(
                        opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                        ref.index()+") instanceof Arg.IntConst");
                }
            }
            System.out.println(") {");
        }
        
        HashSet< String > requiredCtSeen=new HashSet< String >();
        ArrayList< RhsReference > requiredCt=p.requiredConstants();
        
        if (!requiredCt.isEmpty()) {
            System.out.print("if (");
            boolean first=true;
            for (RhsReference ref : requiredCt) {
                if (requiredCtSeen.add(ref.production().name())) {
                    if (first) {
                        first=false;
                    } else {
                        System.out.println(" &&");
                    }
                    System.out.print(
                        opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                        ref.index()+").equals("+ref.production().getConstant()+")");
                }
            }
            System.out.println(") {");
        }

        HashSet< String > requiredNotImSeen=new HashSet< String >();
        ArrayList< RhsReference > requiredNotIm=p.requiredNotImmediates();
        
        if (!requiredNotIm.isEmpty()) {
            System.out.print("if (");
            boolean first=true;
            for (RhsReference ref : requiredNotIm) {
                if (requiredNotImSeen.add(ref.production().name())) {
                    if (first) {
                        first=false;
                    } else {
                        System.out.println(" &&");
                    }
                    System.out.print(
                        opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                        ref.index()+") instanceof Var");
                }
            }
            System.out.println(") {");
        }
        
        HashSet< String > assertImSeen=new HashSet< String >();
        ArrayList< RhsReference > assertIm=p.assertImmediates();
        
        for (RhsReference ref : assertIm) {
            if (assertImSeen.add(ref.production().name())) {
                System.out.println(
                    "if (!("+opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                    ref.index()+") instanceof Arg.IntConst)) {");
                System.out.println(
                    "throw new CompilerException(\"Argument \'"+ref.production().name()+
                    "\' to \'"+p+"\' is not an immediate in: \"+"+
                    opVars.get(varIndices.get(ref.enclosing().varName()))+");");
                System.out.println("}");
            }
        }
        
        HashSet< String > assertCtSeen=new HashSet< String >();
        ArrayList< RhsReference > assertCt=p.assertConstants();
        
        for (RhsReference ref : assertCt) {
            if (assertCtSeen.add(ref.production().name())) {
                System.out.println(
                    "if (!"+opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                    ref.index()+").equals("+ref.production().getConstant()+")) {");
                System.out.println(
                    "throw new CompilerException(\"Argument \'"+ref.production().name()+
                    "\' to \'"+p+"\' does not equal "+ref.production().getConstant()+": \"+"+
                    opVars.get(varIndices.get(ref.enclosing().varName()))+");");
                System.out.println("}");
            }
        }
        
        HashSet< String > assertNotImSeen=new HashSet< String >();
        ArrayList< RhsReference > assertNotIm=p.assertNotImmediates();
        
        for (RhsReference ref : assertNotIm) {
            if (assertNotImSeen.add(ref.production().name())) {
                System.out.println(
                    "if (!("+opVars.get(varIndices.get(ref.enclosing().varName()))+".rhs("+
                    ref.index()+") instanceof Var)) {");
                System.out.println(
                    "throw new CompilerException(\"Argument \'"+ref.production().name()+
                    "\' to \'"+p+"\' is not a variable in: \"+"+
                    opVars.get(varIndices.get(ref.enclosing().varName()))+");");
                System.out.println("}");
            }
        }
        
        System.out.print("if (visit"+Util.majorCamel(key)+"(");
        boolean first=true;
        for (String arg : opVars) {
            if (first) {
                first=false;
            } else {
                System.out.println(",");
            }
            System.out.print(arg);
        }
        System.out.println(")) {");
        
        for (String opVar : opVars) {
            System.out.println("acceptedOperation("+opVar+");");
        }
        
        System.out.println("return true;");
        System.out.println("}");
        
        if (!requiredIm.isEmpty()) {
            System.out.println("}");
        }
        
        if (!requiredCt.isEmpty()) {
            System.out.println("}");
        }
        
        if (!requiredNotIm.isEmpty()) {
            System.out.println("}");
        }
        
        for (int i=0;i<requiredEq.size();++i) {
            System.out.println("}");
        }
    }
    
    static void justTryAllProductions(ArrayList< String > opVars,Set< String > list) {
        for (String key : list) {
            justTryProduction(opVars,key);
        }
    }
    
    static void considerArgIdx(final String curOpVar,
                               ArrayList< String > opVars,
                               final int argIdx,
                               final int numArgs,
                               LinkedHashMap< String, Production > ps,
                               final Continuation cont) {
        // loop through the list of productions and group together contiguous regions of
        // productions for which the arg at argIdx is an operation.  then for those, do
        // a switch.
        
        assert argIdx<=numArgs;
        
        if (argIdx==numArgs) {
            cont.call(opVars,ps);
            return;
        }
        
        ArrayList< LinkedHashMap< String, Production > > list=
            new ArrayList< LinkedHashMap< String, Production > >();
        
        boolean onVars=false; // initial value not used
        
        for (Map.Entry< String, Production > p : ps.entrySet()) {
            try {
                if (list.isEmpty() || p.getValue().args().get(argIdx).isVariable()!=onVars) {
                    LinkedHashMap< String, Production > val=
                        new LinkedHashMap< String, Production >();
                    val.put(p.getKey(),p.getValue());
                    list.add(val);
                    onVars=p.getValue().args().get(argIdx).isVariable();
                } else {
                    list.get(list.size()-1).put(p.getKey(),p.getValue());
                }
            } catch (Throwable e) {
                throw new Error("Failed on "+p.getKey()+" -> "+p.getValue()+", ps = "+ps+", argIdx = "+argIdx+", numArgs = "+numArgs,e);
            }
        }
        
        for (LinkedHashMap< String, Production > span_ : list) {
            final LinkedHashMap< String, Production > span=span_;
            if (span.entrySet().iterator().next().getValue().args().get(argIdx).isVariable()) {
                // we have a span of variables.  that means that the entire span is equivalent
                // on this arg index, but possibly not equivalent on the subsequent ones.  so,
                // recurse with a larger argument index
                
                considerArgIdx(curOpVar,opVars,argIdx+1,numArgs,span,cont);
            } else {
                // we have a span of operations.  that means that we need to first figure out
                // the instruction, from the corresponding rhs index.  this may fail, if
                // the there is no instruction, or if for some reason the user code doesn't
                // want us to see it.
                
                String subOpVarArg=newVar();
                String subOpVarVar=newVar();
                String subOpVar=newVar();
                
                ArrayList< String > subOpVars=new ArrayList< String >(opVars);
                subOpVars.add(subOpVar);
                maxOpVars=Math.max(maxOpVars,subOpVars.size());
                
                System.out.println("Arg "+subOpVarArg+"="+curOpVar+".rhs("+argIdx+");");
                System.out.println("if ("+subOpVarArg+" instanceof Var) {");
                System.out.println("Var "+subOpVarVar+"=(Var)"+subOpVarArg+";");
                System.out.println("Instruction "+subOpVar+"=findInstruction("+subOpVarVar+");");
                System.out.println("if ("+subOpVar+"!=null) {");
                
                System.out.println("opVars[numOpVars+"+(subOpVars.size()-2)+"]="+subOpVarVar+";");
                
                System.out.println("switch ("+subOpVar+".opcode()) {");
                
                for (String opcase : collectHeadsOnArg(span,argIdx)) {
                    System.out.println("case "+opcase+": {");
                    
                    // this will botton-out and call the visit method.
                    // consider the following:
                    //
                    // foo := Add(mul1=Mul(a,b),mul2=Mul(c,d))
                    //
                    // We don't want to call the visit method after finding mul1; we need to
                    // come back and proceed to argIdx=1 and consider mul2 as well.  we
                    // achieve this using continuation passing style.  considerProductions,
                    // as well as considerArgIdx and justTryAllProductions, take a
                    // callback that generates more stuff.
                    
                    considerProductions(
                        subOpVar,subOpVars,pruneOpOnArg(span,argIdx,opcase),
                        new Continuation() {
                            void call(ArrayList< String > changedOpVars,
                                      LinkedHashMap< String, Production > changedSubPS) {
                                // we bottomed out and matched ps, which we need to further
                                // verify on subsequent argument indices.
                                
                                LinkedHashMap< String, Production > changedPS=
                                    new LinkedHashMap< String, Production >();
                                for (String key : changedSubPS.keySet()) {
                                    Production p=span.get(key);
                                    assert p!=null;
                                    changedPS.put(key,p);
                                }
                                
                                considerArgIdx(
                                    curOpVar,changedOpVars,argIdx+1,numArgs,changedPS,cont);
                            }
                        });
                    
                    System.out.println("break;");
                    System.out.println("}");
                }
                
                System.out.println("default: break;");
                System.out.println("}");
                System.out.println("}");
                System.out.println("}");
            }
        }
    }
    
    static void considerProductions(String curOpVar,ArrayList< String > opVars,LinkedHashMap< String, Production > ps,Continuation cont) {
        if (ps.isEmpty()) {
            return;
        }
        
        considerArgIdx(curOpVar,opVars,0,ps.values().iterator().next().args().size(),ps,cont);
    }
    
    public static void main(String[] v) throws Exception {
        Spec s=Spec.parseClass(v[0]);
        
        prods=s.prods;
        
        int lastDot=s.fullClassname.lastIndexOf('.');
        
        String packageName=null;
        String className;
        if (lastDot>=0) {
            packageName=s.fullClassname.substring(0,lastDot);
            className=s.fullClassname.substring(lastDot+1);
        } else {
            className=s.fullClassname;
        }
        
        System.err.println("Processing productions:");
        for (Map.Entry< String, Production > e : prods.entrySet()) {
            System.err.println("   "+e.getKey()+" := "+e.getValue());
        }
        
        // the strategy:
        // 0) validate that for any operation name, all operations of that name have the 
        //    same number of arguments.  also validate that all top-level productions are
        //    operations.
        
        HashMap< String, Integer > opNumArgs=new HashMap< String, Integer >();
        
        for (Map.Entry< String, Production > e : prods.entrySet()) {
            String key=e.getKey();
            Production p=e.getValue();
            
            if (p.isVariable()) {
                System.err.println("Production '"+key+"' is not an operation!");
                System.exit(1);
            }
            
            for (Production op : p.operations()) {
                if (opNumArgs.containsKey(op.name())) {
                    if (opNumArgs.get(op.name())!=op.args().size()) {
                        System.err.println("Operation "+op.name()+" in production "+key+" has "+op.args.size()+" args, but other uses of the operation have "+opNumArgs.get(op.name())+" args!");
                        System.exit(1);
                    }
                } else {
                    opNumArgs.put(op.name(),op.args().size());
                }
            }
        }
        
        System.err.println("All productions appear valid.");
        
        // 1) name all subproductions and variables
        
        for (Production p : prods.values()) {
            p.nameOps();
        }
        
        System.err.println("Operations named as follows:");
        for (Map.Entry< String, Production > e : prods.entrySet()) {
            System.err.println("   "+e.getKey()+" := "+e.getValue());
        }
        
        // should probably start generating some code.
        
        System.out.println("// generated by com.fiji.fivm.bottomup.Main -- DO NOT EDIT!");
        if (packageName!=null) {
            System.out.println("package "+packageName+";");
        }
        if (packageName==null || !packageName.equals("com.fiji.fivm.c1")) {
            System.out.println("import com.fiji.fivm.c1.*;");
        }
        System.out.println("/** Generated from "+v[0]+" */");
        System.out.println("public abstract class "+className+" extends BottomUpVisitor {");
        
        for (Map.Entry< String, Production > e : prods.entrySet()) {
            System.out.println("    /** "+e.getKey()+" := "+e.getValue()+" */");
            System.out.print("protected abstract boolean visit"+Util.majorCamel(e.getKey())+"(");
            boolean first=true;
            for (String op : e.getValue().operationVarNames()) {
                if (first) {
                    System.out.print("Operation ");
                    first=false;
                } else {
                    System.out.println(",");
                    System.out.print("Instruction ");
                }
                System.out.print(op);
            }
            System.out.println(");");
        }
        
        System.out.println();
        
        for (String opcase : collectHeads(prods)) {
            System.out.println("private boolean __handle"+opcase+"(Operation o,int numOpVars) {");

            ArrayList< String > opVars=new ArrayList< String >();
            opVars.add("o");
            
            considerProductions(
                "o",opVars,prune(prods,opcase),
                new Continuation() {
                    void call(ArrayList< String > opVars,
                              LinkedHashMap< String, Production > ps) {
                        justTryAllProductions(opVars,ps.keySet());
                    }
                });
            System.out.println("return false;");
            System.out.println("}");
        }
        
        System.out.println("public boolean acceptImpl(Operation o,int numOpVars) {");
        
        // 2) at top level: switch on the head productions
        //    at inner levels: for each argument index, switch on productions
        //       and then fall back for variables
        // 3) this will reduce the set of productions.  then infer what additional
        //    constraints there are, and check them in order.
        // 4) for the remaining set of productions, simply call into the visitors
        //    in order.
        
        System.out.println("switch (o.opcode()) {");
        for (String opcase : collectHeads(prods)) {
            System.out.println("case "+opcase+":");
            System.out.println("if (__handle"+opcase+"(o,numOpVars)) return true;");
            System.out.println("else break;");
        }
        System.out.println("default: break;");
        System.out.println("}");
        
        System.out.println("this.numOpVars=numOpVars;");
        System.out.println("if (visitDefault(o)) {");
        System.out.println("acceptedOperation(o);");
        System.out.println("return true;");
        System.out.println("}");
        System.out.println("return false;");
        System.out.println("}");

        System.out.println("public "+className+"() {");
        System.out.println("super("+Math.max(0,(maxOpVars-1))+");");
        System.out.println("}");
        System.out.println("}");
        
        System.err.println("Code generated!");
    }
}

