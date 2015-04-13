/*
 * Production.java
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

public class Production {
    String name;
    String varName;
    ArrayList< Production > args;
    
    public Production(String name) {
        this.name=name;
    }
    
    public boolean isVariable() {
        return args==null;
    }
    
    private static boolean isNumber(String s) {
        try {
            int num=Integer.parseInt(s);
            return (""+num).equals(s);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public boolean isImmediate() {
        return isVariable() && name.startsWith("$") && !isNumber(name.substring(1));
    }
    
    public boolean isNotImmediate() {
        return isVariable() && name.startsWith("%");
    }
    
    public boolean isAssertImmediate() {
        return isVariable() && name.startsWith("!$") && !isNumber(name.substring(2));
    }
    
    public boolean isAssertNotImmediate() {
        return isVariable() && name.startsWith("!%");
    }
    
    public boolean isConstant() {
        return name.startsWith("$") && isNumber(name.substring(1));
    }
    
    public boolean isAssertConstant() {
        return name.startsWith("!$") && isNumber(name.substring(2));
    }
    
    public int getConstant() {
        if (name.startsWith("$")) {
            return Integer.parseInt(name.substring(1));
        } else if (name.startsWith("!$")) {
            return Integer.parseInt(name.substring(2));
        } else {
            throw new Error("should not get here");
        }
    }
    
    public boolean isOperation() {
        return args!=null;
    }
    
    public void makeOperation() {
        if (args==null) {
            args=new ArrayList< Production >();
        }
    }
    
    public void addArg(Production p) {
        makeOperation();
        args.add(p);
    }
    
    public void addArgs(Collection< Production > ps) {
        makeOperation();
        args.addAll(ps);
    }
    
    void collectVariables(HashSet< String > result) {
        if (isVariable()) {
            result.add(name);
        } else {
            for (Production p : args) {
                p.collectVariables(result);
            }
        }
    }
    
    public HashSet< String > variables() {
        HashSet< String > result=new HashSet< String >();
        collectVariables(result);
        return result;
    }
    
    void collectOps(ArrayList< Production > result) {
        if (isOperation()) {
            result.add(this);
            for (Production p : args) {
                p.collectOps(result);
            }
        }
    }
    
    public ArrayList< Production > operations() {
        ArrayList< Production > result=new ArrayList< Production >();
        collectOps(result);
        return result;
    }
    
    public ArrayList< String > operationVarNames() {
        ArrayList< String > result=new ArrayList< String >();
        for (Production p : operations()) {
            result.add(p.varName());
        }
        return result;
    }
    
    void collectVarRefs(ArrayList< RhsReference > result) {
        if (isOperation()) {
            for (int i=0;i<args.size();++i) {
                Production p=args.get(i);
                if (p.isVariable()) {
                    result.add(new RhsReference(this,i));
                } else {
                    p.collectVarRefs(result);
                }
            }
        }
    }
    
    public ArrayList< RhsReference > varRefs() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        collectVarRefs(result);
        return result;
    }
    
    /** Returns a mapping from variables to the set of references where that
        variable appears, if that variable appears more than once. */
    public HashMap< String, ArrayList< RhsReference > > requiredEqualities() {
        HashMap< String, ArrayList< RhsReference > > result=
            new HashMap< String, ArrayList< RhsReference > >();
        
        for (RhsReference ref : varRefs()) {
            ArrayList< RhsReference > list=result.get(ref.production().name());
            if (list==null) {
                result.put(ref.production().name(),
                           list=new ArrayList< RhsReference >());
            }
            list.add(ref);
        }
        
        ArrayList< String > toRemove=new ArrayList< String >();
        for (Map.Entry< String, ArrayList< RhsReference > > e
                 : result.entrySet()) {
            if (e.getValue().size()==1) {
                toRemove.add(e.getKey());
            }
        }
        
        for (String name : toRemove) {
            result.remove(name);
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > requiredImmediates() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isImmediate()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > requiredConstants() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isConstant()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > requiredNotImmediates() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isNotImmediate()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > assertImmediates() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isAssertImmediate()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > assertConstants() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isAssertConstant()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    public ArrayList< RhsReference > assertNotImmediates() {
        ArrayList< RhsReference > result=new ArrayList< RhsReference >();
        
        for (RhsReference ref : varRefs()) {
            if (ref.production().isAssertNotImmediate()) {
                result.add(ref);
            }
        }
        
        return result;
    }
    
    void nameOpsStage1(HashMap< String, Integer > result) {
        if (isOperation()) {
            Integer cnt=result.get(Util.minorCamel(name));
            if (cnt==null) {
                varName=Util.minorCamel(name);
                result.put(Util.minorCamel(name),1);
            } else {
                int newCnt=cnt+1;
                varName=Util.minorCamel(name)+newCnt;
                result.put(Util.minorCamel(name),newCnt);
            }
            
            for (Production p : args) {
                p.nameOpsStage1(result);
            }
        }
    }
    
    void nameOpsStage2(HashMap< String, Integer > result) {
        if (isOperation()) {
            if (Util.minorCamel(name).equals(varName) &&
                result.get(Util.minorCamel(name))>1) {
                varName=Util.minorCamel(name)+1;
            }
            while (Util.isIdentifier(varName)) {
                varName+="_";
            }
            
            for (Production p : args) {
                p.nameOpsStage2(result);
            }
        }
    }
    
    public void nameOps() {
        HashMap< String, Integer > map=new HashMap< String, Integer >();
        nameOpsStage1(map);
        nameOpsStage2(map);
    }
    
    public String name() {
        return name;
    }
    
    public ArrayList< Production > args() {
        return args;
    }
    
    public String varName() {
        return varName;
    }
    
    public String toString() {
        StringBuilder result=new StringBuilder();
        if (varName!=null && !varName.equals(Util.minorCamel(name))) {
            result.append(varName);
            result.append("=");
        }
        result.append(name);
        if (isOperation()) {
            result.append("(");
            boolean first=true;
            for (Production p : args()) {
                if (first) {
                    first=false;
                } else {
                    result.append(", ");
                }
                result.append(p);
            }
            result.append(")");
        }
        return result.toString();
    }
}

