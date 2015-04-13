/*
 * Global.java
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Pattern;

import com.fiji.fivm.Settings;
import com.fiji.fivm.SysDepSetting;
import com.fiji.fivm.om.FieldLayerOuter;
import com.fiji.fivm.om.GCMapBuilder;
import com.fiji.fivm.om.OMClass;
import com.fiji.fivm.om.OMData;

import com.fiji.config.*;

public class Global {
    public static final String VERSION=com.fiji.fivm.Config.VERSION;

    public static final String COPYRIGHT=com.fiji.fivm.Config.COPYRIGHT;

    public static String revision=null;
    public static String fivmcHomeDir=null;
    
    public static String fullName() {
        return "fivmc/c1 "+revision+" "+COPYRIGHT+", All Rights Reserved";
    }

    public static String name=null;

    static Analysis analysis=new OpenWorld();
    static Analysis probAnalysis=null;

    static int analysisEpoch=0;

    public static VisibleMethod entrypoint;

    public static RootsRepo rootsRepo;

    public static ObjLocAnalysis ola=new OpenObjLocAnalysis();

    public static CallGraph cg = null;

    public static CodeRepo repo;

    // configuration options ... you may need to change then.
    public static int pointerSize = -1;

    public static int logPageSize = 12;

    public static ByteOrder endianness = ByteOrder.LITTLE_ENDIAN;

    public static RTEMode rteMode = RTEMode.RTE_PER_THROW;

    public static int rtVerbosityLimit = 1;

    public static int rtFlowLogBufsize = 48;

    public static String syncTypeMacroName = null;

    public static PoundDefineRepo pdr = null;

    public static GC gc;

    public static ObjectModel om;

    public static HeaderModel hm;

    public static Library lib;

    public static boolean gcScopedMemory = false;

    public static boolean staticJNI;

    public static boolean dynLoading;

    public static boolean dumbHashCode;

    public static boolean oneShotPayload;

    public static OSFlavor osFlavor;

    public static boolean specializeRuntime=true;

    public static double pollcheckBudget = 100.0;

    public static double unrollBudget = 50.0;

    public static double peelBudget = 150.0;

    public static String noInlineAttribute = "";

    public static boolean profileCheckInit=false;

    public static boolean profileSubtype=false;

    public static boolean profileNullCheck=false;

    public static boolean profileArrayBounds=false;

    public static boolean profileArrayStore=false;

    public static boolean profileCheckDiv=false;

    public static boolean profileInvokes=false;

    public static boolean profileStackHeight=false;

    public static boolean coverage=false;

    public static boolean throwDebug=true;

    public static boolean noPollcheck=false;
    
    public static boolean optPollcheck=false;

    public static boolean doRefMapOpt=true;

    public static boolean blackStack=false;

    public static Pattern verboseRunMethod=null;

    public static boolean doInline = true;

    public static int maxInlineableSize = 150;

    public static int maxInlineCallerBlocks = 1000;

    public static int maxInlineFixpoint = 3;

    public static int maxRecursiveInline = 0;

    public static boolean rce = true;

    public static boolean lateRCE = false;
    
    public static int rceDistance = 100;
    public static int defaultProxLimit = 5;
    
    public static int numForwardPasses = 0;

    public static int omBlocksMax = 10000;

    public static boolean reduceVars=false;

    public static int extraHeaderSize = 0;
    
    public static boolean haveCOffsetsAndSizes=false;
    public static boolean indirectGodGivens=false;
    public static boolean nativeBackend=false;
    
    public static RegAlloc regAlloc;
    public static boolean doRegCoalescing=true;
    public static int floatPretendPersistent=3;
    
    public static boolean measurePhaseTimings=false;
    
    /** What is the offset from the beginning of the object to where the
        pointer points? */
    public static int objectTDOffset = 0;

    /** What is the offset from the beginning of the GC allocated object
        to where the pointer points? */
    public static int objectGCOffset = 0;

    /** What is the allocation offset?  I.e., the offset from the beginning of
        a chunk to where the object pointer points? */
    public static int allocOffset = 0;

    /** The field layer outer that we use. */
    public static FieldLayerOuter fieldLayerOuter = null;

    /** The GC map builder that we use. */
    public static GCMapBuilder gcMapBuilder = null;

    public static CostFunction costFunction = new BasicCostFunction();

    /** Extra RT arguments. */
    public static RTArgOracle extraRTArgs = null;

    // FIXME: change the meaning of this so that verbosity==1 only prints
    // major phases of compilation rather than printing every class.
    public static int verbosity = 2;

    public static HashSet< VisibleMethod > noisyMethods=new HashSet< VisibleMethod >();

    public static boolean runSanityCheck = true;

    public static boolean runWriteReadTest = true;

    public static PrintWriter log = Util.wrapAutoflush(System.err);

    public static NioCoding< VisibleClass > classCoding =
        new NioCoding< VisibleClass >(MapMode.IDENTITY_KEYS);

    public static NioCoding< VisibleMethod > methodCoding =
        new NioCoding< VisibleMethod >(MapMode.IDENTITY_KEYS);

    public static NioCoding< FieldLikeThing > fieldCoding =
        new NioCoding< FieldLikeThing >(MapMode.IDENTITY_KEYS);

    public static NioCoding< Type > typeCoding =
        new NioCoding< Type >(MapMode.IDENTITY_KEYS);

    // revisit map mode...
    public static NioCoding< CField > cfieldCoding =
        new NioCoding< CField >(MapMode.IDENTITY_KEYS);
    
    public static NioCoding< CType > ctypeCoding =
        new NioCoding< CType >(MapMode.IDENTITY_KEYS);

    public static NioCoding< NativeSignature > nsigCoding =
        new NioCoding< NativeSignature >(MapMode.STRUCTURAL_KEYS);

    public static NioCoding< MethodTypeSignature > msigCoding =
        new NioCoding< MethodTypeSignature >(MapMode.STRUCTURAL_KEYS);
    
    public static NioCoding< String > anonymousIDCoding =
        new NioCoding< String >(MapMode.STRUCTURAL_KEYS);

    // this guy is for when we use compression
    public static HashMap< MethodSignature, Integer > imethodSigIndex;
    // and this guy is for when we don't use compression
    public static HashMap< VisibleMethod, Integer > imethodIndex;
    public static int maxImethodIndex;

    private Global() {}
    
    private static Context root;

    private static HashMap< String, Context > contextMap=new HashMap< String, Context >();
    private static HashSet< Context > contexts=new HashSet< Context >();
    private static ArrayList< Context> contextList=new ArrayList< Context >();
    
    public static synchronized void addContext(Context c) {
        assert !contexts.contains(c);
        c.index=contexts.size();
        contexts.add(c);
        contextList.add(c);
        contextMap.put(c.description(),c);
    }
    
    public static Context contextByName(String name) {
        Context c=contextMap.get(name);
        if (c==null) {
            throw new CompilerException("No such context: "+name);
        }
        return c;
    }
    
    public static Set< Context > unorderedContextSet() {
        return Collections.unmodifiableSet(contexts);
    }

    public static List< Context > contextList() {
        return Collections.unmodifiableList(contextList);
    }

    public static void setRoot(Context root) {
        addContext(root);
        Global.root = root;
    }

    public static Context root() {
        return root;
    }
    
    public static VisibleClass getClass(String contextName,
                                        String className) {
        return contextByName(contextName).getClassInThisContext(className);
    }
    
    public static VisibleClass getClass(ResolutionID id) {
        return getClass(id.getContext(),id.getKlass());
    }
    
    public static VisibleClass getClass(ConfigNode classDescr) {
        try {
            return getClass(new ResolutionID(classDescr));
        } catch (CompilerException e) {
            throw new CompilerException(
                "Could not resolve "+classDescr+" in "+
                classDescr.getContextDescription(),
                e);
        }
    }
    
    public static Collection< VisibleClass > allClasses() {
        ArrayList< VisibleClass > result=new ArrayList< VisibleClass >();
        for (Context c : contextList()) {
            result.addAll(c.classes.values());
        }
        return result;
    }

    public static Collection<VisibleClass> resolvedClasses() {
        ArrayList< VisibleClass > result=new ArrayList< VisibleClass >();
        for (Context c : contextList()) {
            result.addAll(c.resolvedClasses());
        }
        return result;
    }
    
    public static Collection< VisibleClass > knownClassesThatMayBeInstantiated() {
	if (analysis().closed()) {
	    return analysis().instantiatedClasses();
	} else {
	    return resolvedClasses();
	}
    }
    
    public static Collection< VisibleClass > knownClassesThatMayBeLive() {
	if (analysis().closed()) {
	    return analysis().liveClasses();
	} else {
	    return allClasses();
	}
    }
    
    public static Collection< Type > knownTypesThatMayBeInstantiated() {
	if (analysis().closed()) {
	    return analysis().instantiatedTypes();
	} else {
	    return Type.allKnownRealTypes();
	}
    }
    
    public static Collection< Type > knownTypesThatMayBeUsed() {
	if (analysis().closed()) {
	    return analysis().usedTypes();
	} else {
	    return Type.allKnownRealTypes();
	}
    }

    public static void forAllMethods(VisibleMethod.Visitor v) {
	if (Global.analysis().closed()) {
	    for (VisibleMethod m : Global.analysis().calledMethods()) {
		v.visit(m);
	    }
	} else {
	    for (VisibleClass c : resolvedClasses()) {
		for (VisibleMethod m : c.methods()) {
		    v.visit(m);
		}
	    }
	}
    }
    
    public static ArrayList< Type > allTypesUsedAtRuntime() {
        ArrayList< Type > result=new ArrayList< Type >();
        for (Context c : contextList()) {
            result.addAll(c.allTypesUsedAtRuntime());
        }
        return result;
    }

    public static ArrayList< Type > allResolvedTypesUsedAtRuntime() {
        ArrayList< Type > result=new ArrayList< Type >();
        for (Context c : contextList()) {
            result.addAll(c.allResolvedTypesUsedAtRuntime());
        }
        return result;
    }

    public static ArrayList< Type > allUnresolvedTypesUsedAtRuntime() {
        ArrayList< Type > result=new ArrayList< Type >();
        for (Context c : contextList()) {
            result.addAll(c.allUnresolvedTypesUsedAtRuntime());
        }
        return result;
    }

    public static ArrayList< Type > allTypesUsedAtRuntimeFor(ResolvedState state) {
        switch (state) {
        case RESOLVED: return allResolvedTypesUsedAtRuntime();
        case UNRESOLVED: return allUnresolvedTypesUsedAtRuntime();
        default: throw new Error("unrecognized: "+state);
        }
    }
    
    /** How big is the object header in bytes? */
    public static int tdHeaderSize() { return OMData.tdHeaderSize(); }

    public static int totalHeaderSize() { return OMData.totalHeaderSize(); }
    
    public static boolean haveNativeBackend() {
        return Settings.X86;
    }
    
    public static void resolveAll() {
        for (Context c : contextList()) {
            c.resolveAll();
        }
    }
    
    public static void fixResolution() {
        for (Context c : contextList()) {
            c.fixResolution();
        }
    }
    
    public static void clearNameCaches() {
        for (Context c : contextList()) {
            c.clearNameCaches();
        }
    }
    
    public static ResolutionReport resolutionReport() {
        ResolutionReport result=new ResolutionReport();
        for (Context c : contextList()) {
            result.addAll(c.resolutionReport);
        }
        Collection< VisibleClass > resolvedClasses=resolvedClasses();
        for (VisibleClass target : allClasses()) {
            if (!target.resolved()) {
                if (verbosity>=2) {
                    Global.log.println("Unresolved class: "+target);
                }
                for (VisibleClass use : resolvedClasses) {
                    for (OMClass potential : use.directSupertypes()) {
                        if (potential==target) {
                            result.addUse(target.getResolutionID(),use.getResolutionID());
                        }
                    }
                    for (VisibleMethod vm : use.methods()) {
                        if (vm.getType().hasUnderlyingClass() &&
                            vm.getType().getUnderlyingClass()==target) {
                            result.addUse(target.getResolutionID(),vm.getResolutionID());
                        }
                        for (Type t : vm.getParams()) {
                            if (t.hasUnderlyingClass() &&
                                t.getUnderlyingClass()==target) {
                                result.addUse(target.getResolutionID(),vm.getResolutionID());
                            }
                        }
                    }
                    for (VisibleField vf : use.fields()) {
                        if (vf.getType().hasUnderlyingClass() &&
                            vf.getType().getUnderlyingClass()==target) {
                            result.addUse(target.getResolutionID(),vf.getResolutionID());
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public static void setAttributes() {
        for (Context c : contextList()) {
            c.setAttributes();
        }
    }
    
    public static void hierarchyVerification() {
        for (Context c : contextList()) {
            c.hierarchyVerification();
        }
    }

    // assume that no further classes may ever be introduced
    public static void close() {
        for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
            if (c.hasSuperclass()) {
                c.getSuperclass().knownDirectSubs.add(c);
                c.getSuperclass().knownSubs.add(c);
            }
            for (VisibleClass i : c.getSuperInterfaces()) {
                i.knownDirectSubs.add(c);
                i.knownSubs.add(c);
            }
        }
        boolean changed=true;
        while (changed) {
            changed=false;
            for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
                HashSet< VisibleClass > toAdd=new HashSet< VisibleClass >();
                for (VisibleClass c2 : c.knownSubs) {
                    for (VisibleClass c3 : c2.knownSubs) {
                        if (!c.knownSubs.contains(c3)) {
                            toAdd.add(c3);
                        }
                    }
                }
                changed|=c.knownSubs.addAll(toAdd);
            }
        }
        
        // kludge ... make sure that what the analysis says are live and instantiated
        // classes, are actually also known to the typesystem
        for (VisibleClass c : knownClassesThatMayBeLive()) {
            c.asType();
        }
        
        Type.closeTypes();
    }

    public static Analysis analysis() {
        return analysis;
    }

    public static void setAnalysis(Analysis a) {
        // clear cached fields that would be invalidated by the analysis.
        for (VisibleClass c : resolvedClasses()) {
            c.vtable=null;
            c.resolution=null;
            c.allInstanceMethods=null;
            c.dynableMethods=null;
            c.exactDispatch=null;
            c.dispatch=null;
            c.hasShouldCallCheckInit=false;
        }
        
        // now set the analysis.
        analysis=a;
        
        for (VisibleClass c : resolvedClasses()) {
            c.recomputeMemberIndices();
        }
        
        analysisEpoch++;
    }

    public static int analysisEpoch() {
        return analysisEpoch;
    }
    
    public static Analysis hasProbAnalysis() {
        return probAnalysis;
    }
    
    public static Analysis probAnalysis() {
        if (probAnalysis==null) {
            return analysis();
        } else {
            return probAnalysis;
        }
    }
    
    public static void setProbAnalysis(Analysis a) {
        probAnalysis=a;
    }
    
    static class IMethodSigNode extends GraphColoring.Node {
	MethodSignature s;
	
	IMethodSigNode(MethodSignature s) {
	    this.s=s;
	}
        
        public String description() {
            return s.toString();
        }
    }
    
    static class IMethodNode extends GraphColoring.Node {
	VisibleMethod m;
	
	IMethodNode(VisibleMethod m) {
	    this.m=m;
	}
        
        public String description() {
            return m.canonicalName();
        }
    }
    
    public static void pickImethodIndices() {
        long before=System.currentTimeMillis();
        long subBefore;
        if (Settings.ITABLE_COMPRESSION) {
            assert analysis().closed();
            LinkedHashMap< MethodSignature, IMethodSigNode > imethods=
                new LinkedHashMap< MethodSignature, IMethodSigNode >();
            for (VisibleClass c : analysis().instantiatedClasses()) {
                if (c.isInterface()) {
                    for (Map.Entry< MethodSignature, VisibleMethod > e
                             : c.methods.entrySet()) {
                        if (e.getValue().isInstance() &&
                            !imethods.containsKey(e.getKey()) &&
                            analysis().isCalled(e.getValue())) {
                            imethods.put(e.getKey(),new IMethodSigNode(e.getKey()));
                        }
                    }
                }
            }
            if (verbosity>=1) {
                log.println("Number of interface methods: "+imethods.size());
            }
            if (verbosity>=3) {
                log.println("Interface method signatures: "+imethods.keySet());
            }
            subBefore=System.currentTimeMillis();
            for (VisibleClass c : analysis().instantiatedClasses()) {
                if (!c.isInterface() && c.hasInstances()) {
                    ArrayList< IMethodSigNode > curIMethods=new ArrayList< IMethodSigNode >();
                    for (VisibleMethod m : c.allInterfaceMethods()) {
                        curIMethods.add(imethods.get(m.getSignature()));
                    }
                    GraphColoring.cluster(curIMethods);
                }
            }
            if (verbosity>=1) {
                long subAfter=System.currentTimeMillis();
                log.println("generated graph in "+(subAfter-subBefore)+" ms");
            }
            int maxColor=GraphColoring.color(imethods.values());
            if (verbosity>=1) {
                log.println("max color = "+maxColor);
            }
            imethodSigIndex=new HashMap< MethodSignature, Integer >();
            for (IMethodSigNode n : imethods.values()) {
                imethodSigIndex.put(n.s,n.color());
            }
            maxImethodIndex=maxColor;
        } else {
            LinkedHashMap< VisibleMethod, IMethodNode > imethods=
                new LinkedHashMap< VisibleMethod, IMethodNode >();
            for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
                if (c.isInterface()) {
                    for (VisibleMethod m : c.methods.values()) {
                        if (m.isInstance() &&
                            !imethods.containsKey(m) &&
                            analysis().isCalled(m)) {
                            imethods.put(m,new IMethodNode(m));
                        }
                    }
                }
            }
            for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
                if (!c.isInterface() && c.hasInstances()) {
                    ArrayList< IMethodNode > curIMethods=new ArrayList< IMethodNode >();
                    for (VisibleMethod m : c.allInterfaceMethods()) {
                        curIMethods.add(imethods.get(m));
                    }
                    GraphColoring.cluster(curIMethods);
                }
            }
            int maxColor=GraphColoring.color(imethods.values());
            if (verbosity>=1) {
                log.println("max color = "+maxColor);
            }
            imethodIndex=new HashMap< VisibleMethod, Integer >();
            for (IMethodNode n : imethods.values()) {
                imethodIndex.put(n.m,n.color());
            }
            maxImethodIndex=maxColor;
        }
        subBefore=System.currentTimeMillis();
        for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
            c.buildITable();
        }
        if (verbosity>=1) {
            long after=System.currentTimeMillis();
            log.println("building itables took "+(after-subBefore)+" ms");
            log.println("handling interface methods took "+(after-before)+" ms");
        }
    }
    
    public static void handleVirtualDispatch() {
        pickImethodIndices();
        handleVirtualMethods();
    }
    
    public static void forAllCode(Code.Visitor cv) {
        for (VisibleClass vc : knownClassesThatMayBeLive()) {
            if (vc.resolved()) {
        	for (Code c : repo.codeForClass(vc)) {
        	    cv.visit(c);
        	}
            }
        }
        cv.doneVisiting();
    }
    
    // warning: this ignores vcv's return value
    public static void forAllClassesParallel(final VisibleClass.Visitor vcv,
                                             int numJobs) {
        final Iterator< VisibleClass > i=knownClassesThatMayBeLive().iterator();
        Util.startJoin(
            numJobs,
            new HappyRunnable() {
                public void doStuff() throws Throwable {
                    for (;;) {
                        VisibleClass c=Util.synchronizedSuck(i);
                        if (c==null) {
                            break;
                        }
                        vcv.visit(c);
                    }
                }
                public String toString() {
                    return "forAllClassesParallel";
                }
            });
    }
    
    public static void forAllCodeParallel(final Code.Visitor cv,
                                          int numJobs) {
        forAllClassesParallel(
            new VisibleClass.Visitor() {
                public boolean visit(VisibleClass c) {
                    if (c.resolved()) {
                        for (Code code : repo.codeForClass(c)) {
                            cv.visit(code);
                        }
                    }
                    return true;
                }
            },
            numJobs);
    }

    public static Basetype makeResult(Type result) {
        return result.effectiveBasetype().pointerifyObject;
    }

    static void setBaseParams(Basetype[] result,
        			      int offset,
        			      Type[] origParams) {
        for (int i=0;i<origParams.length;++i) {
            result[i+offset]=
        	origParams[i].effectiveBasetype().pointerifyObject;
        }
    }

    public static Basetype[] makeBareParams(Type[] origParams) {
        Basetype[] result=new Basetype[origParams.length];
        setBaseParams(result,0,origParams);
        return result;
    }

    public static Basetype[] makeParams(MethodTypeSignature msig,Type[] origParams) {
        Basetype[] params=
            new Basetype[origParams.length+extraRTArgs.extraRTArgs(msig).length];
        System.arraycopy(extraRTArgs.extraRTArgs(msig),0,
        		 params,0,
        		 extraRTArgs.extraRTArgs(msig).length);
        setBaseParams(params,extraRTArgs.extraRTArgs(msig).length,origParams);
        return params;
    }

    public static NativeSignature makeNativeSig(MethodTypeSignature msig,
        					Type result,
        					Type[] params) {
        return new NativeSignature(makeResult(result),
        			   makeParams(msig,params),
        			   msig);
    }

    public static void dumpCConfig(PrintWriter w) {
        assert specializeRuntime;
        
        try {
            w.println("/* generated by Context.java.  DO NOT EDIT! */");
            for (Field f : Settings.class.getFields()) {
                if (!f.isSynthetic()) {
                    if (f.isAnnotationPresent(SysDepSetting.class)) {
                        w.println("#define FIVMR_"+f.getName()+" "+
                                  (f.getBoolean(null)?"1":"0"));
                    } else {
                        w.println("#define FIVMBUILD_FORCE__"+f.getName()+" 1");
                        w.println("#define FIVMBUILD__"+f.getName()+" "+
                                  (f.getBoolean(null)?"1":"0"));
                    }
                }
            }
            w.println("#define FIVMBUILD_SPECIAL_GC 1");
            w.println("#define FIVMBUILD_GC_NAME fivmr_SpecialGC");
            w.println("#define FIVMSYS_PTRSIZE "+pointerSize);
            w.println("#define FIVMSYS_LOG_PAGE_SIZE "+logPageSize);
            w.println("#define FIVMSYS_NOINLINE "+noInlineAttribute);
            w.println("#define FIVMR_LOG_LEVEL "+rtVerbosityLimit);
            w.println("#define FIVMR_SYNC_"+syncTypeMacroName+" 1");
	    w.println("#define FIVMR_FLOWLOG_BUFFER_ENTRIES "+rtFlowLogBufsize);
            w.println("#define FIVMC_VERSION \""+Util.cStringEscape(revision)+"\"");
        } catch (Throwable t) {
            Util.rethrow(t);
        }
    }

    public static void dumpCConfig(String filename) throws IOException {
        PrintWriter fout=Util.wrap(new FileOutputStream(filename));
        try {
            dumpCConfig(fout);
        } finally {
            fout.close();
        }
    }

    public static void handleVirtualMethods() {
        long before=System.currentTimeMillis();
        for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
            c.buildVTable();
        }
        long after=System.currentTimeMillis();
        if (verbosity>=1)
            log.println("handling virtual methods took "+(after-before)+" ms");
    }

    public static void handleFields(FieldLayerOuter flo) {
        long before=System.currentTimeMillis();
        for (VisibleClass c : knownClassesThatMayBeInstantiated()) {
            c.layOutFields(flo);
        }
        long after=System.currentTimeMillis();
        if (verbosity>=1)
            log.println("handling fields took "+(after-before)+" ms");
    }

    public static void handleFields() {
        handleFields(fieldLayerOuter);
    }
    
}

