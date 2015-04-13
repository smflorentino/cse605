/*
 * RunC1.java
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

package com.fiji.fivm.util;

import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

import com.fiji.config.*;

import com.fiji.fivm.Settings;
import com.fiji.fivm.Constants;
import com.fiji.fivm.config.Configuration;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.om.BasicFieldLayerOuter;
import com.fiji.fivm.om.BasicGCMapBuilder;
import com.fiji.fivm.om.FragmentedFieldLayerOuter;
import com.fiji.fivm.om.FragmentedGCMapBuilder;

public class RunC1 {
    private RunC1() {}
    
    public static void main(String[] v) throws Throwable {
	long fullBefore=System.currentTimeMillis();
	
	if (v.length!=1) {
	    throw new Error("Expected exactly one argument: the name of the configuration file.");
	}
	
	Configuration config=new Configuration(v[0]);
	
	Global.verbosity=config.getVerbosity();
        Global.name=config.getOutput();
        
        if (config.getBuildType().equals("EXE")) {
            Global.specializeRuntime=true;
        } else if (config.getBuildType().equals("PAYLOAD")) {
            Global.specializeRuntime=false;
        } else {
            throw new Error("bad value for buildType: "+config.getBuildType());
        }
        
        Global.oneShotPayload=config.getOneShotPayload();
        Global.revision=config.getRevision();
        Global.fivmcHomeDir=config.getFivmcHomeDir();
	Global.runSanityCheck=config.getRunSanityCheck();
        Global.runWriteReadTest=config.getRunWriteReadTest();
	Global.pointerSize=config.getPointerSize();
        Global.logPageSize=config.getLogPageSize();
        Global.rtVerbosityLimit=config.getRTVerbosityLimit();
        Global.rtFlowLogBufsize=config.getRTFlowLogBufsize();
        Global.syncTypeMacroName=config.getSyncTypeMacroName();

	if (config.getEndianness().equals("little")) {
	    Global.endianness=ByteOrder.LITTLE_ENDIAN;
	} else if (config.getEndianness().equals("big")) {
	    Global.endianness=ByteOrder.BIG_ENDIAN;
	} else {
	    throw new Error("bad value for endianness: "+config.getEndianness());
	}

	Global.maxInlineFixpoint=config.getMaxInlineFixpoint();
	Global.maxRecursiveInline=config.getMaxInlineRecurse();
	Global.maxInlineCallerBlocks=config.getMaxInlineCallerBlocks();
	Global.maxInlineableSize=config.getMaxInlineableSize();
        Global.omBlocksMax=config.getOMBlocksMax();
	Global.noInlineAttribute=config.getNoInlineAttribute();
	Global.doInline=config.getDoInline();
	Global.rce=config.getRCE();
        Global.rceDistance=config.getRCEDistance();
	Global.lateRCE=config.getLateRCE();
        Global.numForwardPasses=config.getNumForwardPasses();
	Global.reduceVars=config.getReduceVars();
	Global.noPollcheck=config.getNoPollcheck();
	Global.optPollcheck=config.getOptPollcheck();
        Global.pollcheckBudget=config.getPollcheckBudget();
        Global.unrollBudget=config.getUnrollBudget();
        Global.peelBudget=config.getPeelBudget();
	Global.profileCheckInit=config.getProfileCheckInit();
	Global.profileSubtype=config.getProfileSubtype();
	Global.profileNullCheck=config.getProfileNullCheck();
	Global.profileArrayStore=config.getProfileArrayStore();
	Global.profileArrayBounds=config.getProfileArrayBounds();
	Global.profileCheckDiv=config.getProfileCheckDiv();
	Global.profileInvokes=config.getProfileInvokes();
	Global.profileStackHeight=config.getProfileStackHeight();
	Global.coverage=config.getCoverage();
	Global.throwDebug=config.getThrowDebug();
	Global.staticJNI=config.getStaticJNI();
	Global.dynLoading=config.getDynLoading();
	Global.dumbHashCode=config.getDumbHashCode();
        Global.doRefMapOpt=config.getDoRefMapOpt();
        Global.blackStack=config.getBlackStack();
	Global.gcScopedMemory=config.getGCScopedMemory();
        Global.measurePhaseTimings=config.getMeasurePhaseTimings();
        Global.indirectGodGivens=config.getIndirectGodGivens();
        Global.nativeBackend=config.getNativeBackend();
        
        if (config.getRegAlloc().equals("LOCAL_LINEAR_SCAN")) {
            Global.regAlloc=RegAlloc.LOCAL_LINEAR_SCAN;
        } else if (config.getRegAlloc().equals("ITERATIVE_REGISTER_COALESCING")) {
            Global.regAlloc=RegAlloc.ITERATIVE_REGISTER_COALESCING;
        } else {
            throw new Error("bad choice of RegAlloc: "+config.getRegAlloc());
        }
        
        Global.doRegCoalescing=config.getDoRegCoalescing();
        
        if (Global.lateRCE) {
            assert Global.rce;
        }
        
        for (String s : config.getSettings()) {
            Settings.class.getField(s).setBoolean(null,true);
        }

	if (config.getVerboseRunMethod().equals("")) {
	    Global.verboseRunMethod=null;
	} else {
	    Global.verboseRunMethod=Pattern.compile(config.getVerboseRunMethod());
	}
	
	if (config.getHeaderModel().equals("narrow")) {
	    Global.hm=HeaderModel.NARROW;
	} else if (config.getHeaderModel().equals("poisoned")) {
	    Global.hm=HeaderModel.POISONED;
	} else {
	    throw new Error("bad value for headerModel: "+config.getHeaderModel());
	}
        
        if (config.getObjectModel().equals("CONTIGUOUS")) {
            Global.om=ObjectModel.CONTIGUOUS;
        } else if (config.getObjectModel().equals("FRAGMENTED")) {
            Global.om=ObjectModel.FRAGMENTED;
        } else {
            throw new Error("bad value for objectModel: "+config.getObjectModel());
        }
	
	if (config.getGC().equals("NONE")) {
	    Global.gc=GC.NOGC;
	} else if (config.getGC().equals("CMR")) {
	    Global.gc=GC.CMRGC;
        } else if (config.getGC().equals("HF")) {
            Global.gc=GC.HFGC;
	} else {
	    throw new Error("bad value for GC: "+config.getGC());
	}
	
	if (config.getOSFlavor().equals("POSIX")) {
	    Global.osFlavor=OSFlavor.POSIX;
	} else if (config.getOSFlavor().equals("RTEMS")) {
	    Global.osFlavor=OSFlavor.RTEMS;
        } else if (config.getOSFlavor().equals("WIN32")) {
            Global.osFlavor=OSFlavor.WIN32;
	} else {
	    throw new Error("bad value for OS flavor: "+config.getOSFlavor());
	}
	
	if (config.getLibrary().equals("FIJICORE")) {
	    Global.lib=Library.FIJICORE;
	} else if (config.getLibrary().equals("GLIBJ")) {
	    Global.lib=Library.GLIBJ;
	} else {
	    throw new Error("bad value for library: "+config.getLibrary());
	}
	
	if (!config.getExtractPoundDefines()) {
	    Global.pdr=new PoundDefineRepo(config.getPoundDefines());
	}
	
	if (Global.verbosity>=1) Global.log.println(Global.fullName());
	
        // NOTE: this also inits Basetype
        CTypesystemReferences.addGenerated(Global.name);
        
        if (config.getHaveCOffsetsAndSizes()) {
            CTypesystemReferences.fromConfigNode(config.getCTypes());
            CTypesystemReferences.lock();
            Global.haveCOffsetsAndSizes=true;
        }

	Type.willClose();
	
        final Context rootContext=new Context(
            "Root",
            new UpResolver() {
                public VisibleClass resolveUp(Context c,
                                              String name) {
                    return null;
                }
            },
            UnresolvedClassCreator.instance);
	Global.setRoot(rootContext);
        
        final Context appContext=new Context(
            "App",
            new UpResolver() {
                public VisibleClass resolveUp(Context c,
                                              String name) {
                    VisibleClass result=rootContext.tryGetClass(name);
                    if (result!=null) {
                        result.extraFlags|=Constants.TBF_OVERRIDE_APP;
                    }
                    return result;
                }
            },
            UnresolvedClassCreator.instance);
        Global.addContext(appContext);
        
        for (String s : config.getLinkedPayloads()) {
            Payload.addSubPayload(s);
        }
	
	if (Global.verbosity>=1) Global.log.println("reading bytecode and creating classes");
	
	for (String file : config.getRootFiles()) {
	    rootContext.classFileIterator().addClassOrJar(file);
	}
	for (String file : config.getAppFiles()) {
	    appContext.classFileIterator().addClassOrJar(file);
	}

        if (Global.verbosity>=1) Global.log.println("parsing bytecode in "+Global.contextList());
	long before=System.currentTimeMillis();
	Global.resolveAll();
        if (Global.verbosity>=1) {
            long after=System.currentTimeMillis();
            Global.log.println("parsed bytecode in "+Global.contextList()+" in "+(after-before)+" ms");
        }

        ResolutionReport firstReport=Global.resolutionReport();
        firstReport.asConfigNode().save(new File("resolutionReport1.conf"));
        firstReport.removeAll(new ResolutionReport(config.getIgnoreErrors()));
        firstReport.printReport(Global.log);

	if (Global.verbosity>=1) Global.log.println("setting up context configuration");
        Type.initFundamentals();
        
	if (config.getHasMain()) {
            Global.entrypoint=
                appContext.resolveMethod(
                    "L"+config.getMain()+";/main([Ljava/lang/String;)V");
            assert Global.entrypoint!=null;
	}
	
	for (String desc : config.getNoisyMethods()) {
	    Global.noisyMethods.add(appContext.resolveMethod(desc));
	}
	
	Global.rootsRepo=new RootsRepo(appContext,
                                       Global.entrypoint,
                                       config.getReflect());
	
	switch (Global.om) {
        case CONTIGUOUS:
            Global.gcMapBuilder=new BasicGCMapBuilder();
            Global.fieldLayerOuter=new BasicFieldLayerOuter();
            ThreadStatePassingCallingConvention.init();
            OneWordHeaderContiguousClasspathObjectModel.init(Global.rootsRepo);
            break;
        case FRAGMENTED:
            FragmentedObjectRepresentation.init();
            Global.gcMapBuilder=new FragmentedGCMapBuilder();
            Global.fieldLayerOuter=new FragmentedFieldLayerOuter();
            ThreadStatePassingCallingConvention.init();
            FragmentedClasspathObjectModel.init(Global.rootsRepo);
            break;
        default: throw new Error();
        }
        
        if (Global.verbosity>=1) {
            Global.log.println("Object model configuration:");
            Global.log.println("   object model: "+Global.om);
            Global.log.println("   header model: "+Global.hm);
            Global.log.println("   objectTDOffset: "+Global.objectTDOffset);
            Global.log.println("   objectGCOffset: "+Global.objectGCOffset);
            Global.log.println("   field layout: "+Global.fieldLayerOuter);
            Global.log.println("   GC map: "+Global.gcMapBuilder);
        }
        
        /* Remove root class methods (but not app methods) from the flow
         * logging domain. */
        rootContext.setNoFlowLog(true);

        Global.setAttributes();
        Global.hierarchyVerification();
	
	if (Global.verbosity>=1) Global.log.println("context configuration in place.");
	
        Global.repo=new ZeroFootprintCodeRepo();
	
	if (Global.verbosity>=1) Global.log.println("performing analysis and closing typesystem...");
	if (config.getAnalysis().equals("0CFA") ||
            config.getAnalysis().equals("E0CFA")) {
	    if (config.getPreprocessOnce()) {
                if (Settings.LOG_CODE_REPO_ACCESSES) {
                    Global.repo=new LoggingCodeRepo("codeRepo.log");
                } else if (Settings.PARALLEL_C1 && config.getJobs()>1) {
                    Global.repo=new ParallelCachingCodeRepo(config);
                } else {
                    Global.repo=new CachingCodeRepo();
                }
	    }
            
            SimpleZeroCFA szcfa=
                new SimpleZeroCFA(Global.repo,
                                  Global.rootsRepo);
            
            if (Global.repo instanceof ParallelCachingCodeRepo) {
                // turn the repo into just a plain caching code repo.
                ((ParallelCachingCodeRepo)Global.repo).stopAsynchrony();
            }
            
	    Global.setAnalysis(szcfa);
	} else if (config.getAnalysis().equals("CHA")) {
	    Global.setAnalysis(new CHA());
        } else if (config.getAnalysis().equals("OPEN")) {
            if (Global.verbosity>=1) {
                Global.log.println("Continuing compilation under open world assumption.");
            }
            // what follows is a *slight* hack...
            if (Global.verbosity>=1) {
                Global.log.println("Loading all methods to ensure we have full type information.");
            }
            Global.forAllCodeParallel(new Code.Visitor() {
                    public void visit(Code c) {
                        // do nothing
                    }
                },
                config.getJobs());
        } else if (config.getAnalysis().equals("O0CFA")) {
            CodeRepo repo=Global.repo;
            
	    if (config.getPreprocessOnce()) {
                if (Settings.LOG_CODE_REPO_ACCESSES) {
                    repo=new LoggingCodeRepo("codeRepo.log");
                } else if (Settings.PARALLEL_C1 && config.getJobs()>1) {
                    repo=new ParallelCachingCodeRepo(config);
                } else {
                    repo=new CachingCodeRepo();
                }
	    }
            
            // any classes that:
            // 
            // - have intrinsics
            // - have methods that use intrinsics
            // - make reference to unboxed types
            //
            // should be added to the root roots repo.  their methods and fields
            // should be added as well.
            //
            // and by "intrinsics" we mean @Import calls as well.
            //
            // or... we could juts make the baseline JIT support these things.
            // of course that will be hard.  Magic, CType, etc.  better approach:
            // force classes that use that junk to be marked @UsesMagic.
            
            for (VisibleClass c : Global.resolvedClasses()) {
                if (c.usesMagic()) {
                    Global.rootsRepo.rootRoots().fullReflect(c);
                }
            }
            
            SimpleZeroCFA szcfa=
                new SimpleZeroCFA(repo,
                                  Global.rootsRepo);
            
            if (Global.verbosity>=1) {
                Global.log.println("Dumping used types according to analysis...");
            }
            
            szcfa.dumpUsedTypes("usedTypes.txt");
            
            Global.setProbAnalysis(szcfa);

            if (Global.verbosity>=1) {
                Global.log.println("Canceling resolution of classes not found to be used.");
            }

            ArrayList< VisibleClass > toCancel=new ArrayList< VisibleClass >();
            
            for (VisibleClass c : Global.allClasses()) {
                if (!szcfa.isUsed(c.asType())) {
                    toCancel.add(c);
                }
            }
            
            for (VisibleClass c : toCancel) {
                c.cancelResolution();
            }
            
            Global.fixResolution();
            
            // the analysis changes a lot of things - that's why we don't reuse
            // the code repo used by the analysis (since any code in it is now
            // invalid), and why we call clearNameCaches(), which cleares out
            // any cached resolution names.  Resolution names contain MD5 hashes
            // of type meta-data, which may include the resolution state of
            // types.
            
            // FIXME: it might be worthwhile to completely drop on the floor
            // any canceled types that are not referenced from other types.
            // this will save on memory usage.  or not.
            
            Global.clearNameCaches();

            if (Global.verbosity>=1) {
                Global.log.println("Continuing compilation under open world assumption.");
            }
	} else {
	    throw new Error("bad value for analysis: "+config.getAnalysis());
	}
        
	Global.close();

        ResolutionReport secondReport=Global.resolutionReport();
        secondReport.asConfigNode().save(new File("resolutionReport2.conf"));
        secondReport.removeAll(new ResolutionReport(config.getIgnoreErrors()));
        secondReport.removeAll(firstReport);
        secondReport.printReport(Global.log);

        if (Global.specializeRuntime) {
            Global.dumpCConfig("fivmc_c1_config.h");
        }

	if (config.getExtractPoundDefines()) {
	    PoundDefineExtractor pde=new PoundDefineExtractor();
	    Global.forAllCode(pde);
	    pde.generateCode("poundDefineExtractor.c");
	    System.exit(0);
	}
        
        if (config.getExtractCTypesystemReferences()) {
            if (Global.verbosity>=1) {
                Global.log.println("Gathering uses of C fields.");
            }
            Global.forAllCodeParallel(new Code.Visitor() {
                    public void visit(Code c) {
                        new NotifyCFieldUses(c).doit();
                    }
                },
                config.getJobs());
            CTypesystemReferences.asConfigNode().save(new File("ctypesystemReferences.conf"),
                                                      "generated by "+Global.fullName());
            System.exit(0);
        }
	
	if (Global.analysis().closed()) {
	    if (Global.verbosity>=1) Global.log.println("dumping results of analysis...");
	    before=System.currentTimeMillis();
	    Global.analysis().dumpUsedTypes("usedTypes.txt");
	    Global.analysis().dumpInstantiatedTypes("instantiatedTypes.txt");
	    Global.analysis().dumpCalledMethods("calledMethods.txt");
	    Global.analysis().dumpExecutedMethods("executedMethods.txt");
	    Global.analysis().dumpLiveFields("liveFields.txt");
	    Global.analysis().dumpExecutedMethodsDetail("executedMethodsDetail.txt");
	    Global.analysis().dumpLiveFieldsDetail("liveFieldsDetail.txt");
	}
        
        final PrintWriter mnmfout=Util.wrap(new FileOutputStream("methodNameMapping.xml"));
        try {
            mnmfout.println("<FIJIMethodNameMapping>");
            Global.forAllMethods(new VisibleMethod.Visitor(){
                    public void visit(VisibleMethod vm) {
                        mnmfout.println("   <Method>");
                        mnmfout.println("      <BytecodeSignature>"+
                                        Util.escapeXMLData(vm.jniName())+
                                        "</BytecodeSignature>");
                        mnmfout.println("      <CFunctionName>"+
                                        Util.escapeXMLData(vm.mangledName())+
                                        "</CFunctionName>");
                        mnmfout.println("   </Method>");
                    }
                });
            mnmfout.println("</FIJIMethodNameMapping>");
        } finally {
            mnmfout.close();
        }
        
	if (Global.verbosity>=1) {
	    long after=System.currentTimeMillis();
	    Global.log.println("dumping results of analysis took "+(after-before)+" ms");
	}
	
        if (config.getDumpCallGraph()) {
            if (Global.verbosity>=1) Global.log.println("Computing call graph...");
            before=System.currentTimeMillis();
            Global.cg=new CallGraph();
            Global.forAllCode(Global.cg);
            if (Global.verbosity>=1) Global.log.println("Dumping call graph...");
            Global.cg.dump("callGraph");
            if (Global.verbosity>=1) {
                long after=System.currentTimeMillis();
                Global.log.println("computed and dumped in "+(after-before)+" ms");
            }
        }
	
	if (config.getObjLocAnalysis().equals("FULL")) {
	    if (Global.verbosity>=1) Global.log.println("Computing object locations...");
	    before=System.currentTimeMillis();
	    Global.ola=new FullObjLocAnalysis();
	    if (Global.verbosity>=1) {
		long after=System.currentTimeMillis();
		Global.log.println("computed in "+(after-before)+" ms");
	    }
	} else if (config.getObjLocAnalysis().equals("SCJ")) {
	    if (Global.verbosity>=1) Global.log.println("Using object location analysis for SCJ.");
	    Global.ola=new SCJObjLocAnalysis();
	} else if (config.getObjLocAnalysis().equals("TRUSTED")) {
            if (Global.verbosity>=1) Global.log.println("Using TRUSTED object location analysis.  Stack allocation disabled for non-Root contexts.");
            Global.ola=new TrustedObjLocAnalysis();
        } else if (config.getObjLocAnalysis().equals("OPEN")) {
            if (Global.verbosity>=1) Global.log.println("Assuming open world object location analysis.");
        } else {
            throw new Error("Invalid object location analysis: "+config.getObjLocAnalysis());
        }

	if (Global.verbosity>=2) Global.log.println("type cons map = "+Type.numTypesCreated());

	Global.handleVirtualDispatch();
	Global.handleFields();
	Type.buildUnifiedTypeTests();
	
	if (Global.verbosity>=1) {
	    int numInterfaces=0;
	    int numClasses=0;
	    int numClassesWithInterfaces=0;
	    int numItableCells=0;
	    for (VisibleClass c : Global.knownClassesThatMayBeInstantiated()) {
		if (c.isInterface()) numInterfaces++;
		else if (!c.isAbstract()) {
		    numClasses++;
		    for (VisibleClass cur=c;cur!=null;cur=cur.getSuperclass()) {
			if (!cur.getSuperInterfaces().isEmpty()) {
			    numClassesWithInterfaces++;
			    break;
			}
		    }
		    if (c.maxITableIndex>=c.minITableIndex) {
			numItableCells+=(c.maxITableIndex-c.minITableIndex+1);
			//numItableCells+=maxCell+1;
		    }
		}
	    }
            if (Settings.ITABLE_COMPRESSION) {
                Global.log.println("Using ITable Compression.");
            } else {
                Global.log.println("Using Plain ITables.");
            }
	    Global.log.println("Num interfaces: "+numInterfaces);
	    Global.log.println("Num concrete classes: "+numClasses);
	    Global.log.println("Num concrete classes with interfaces: "+numClassesWithInterfaces);
	    Global.log.println("Num itable cells: "+numItableCells);
	}
        
        if (config.getStopAfterTypes()) {
            System.err.println("Stopping after computing type information.");
            System.exit(0);
        }
	
	if (Global.doInline) {
	    if (Global.verbosity>=1) Global.log.println("preprocessing inlineables...");
	    long totalBefore=System.currentTimeMillis();
	    final int[] numInlineables=new int[1];
            Global.forAllClassesParallel(
                new VisibleClass.Visitor() {
                    public boolean visit(VisibleClass c) {
                        if (c.resolved()) {
                            HashSet< MethodSignature > sigs=new HashSet< MethodSignature >();
                            for (VisibleMethod m : c.methods()) {
                                // FIXME: have better heuristics, especially for exceptions.
                                // there is pretty much no point in inlining exception constructors.
                                if ((m.codeSize()<=Global.maxInlineableSize ||
                                     m.inlineMode()==InlineMode.MUST_INLINE) &&
                                    m.inlineMode()!=InlineMode.NO_INLINE) {
                                    sigs.add(m.getSignature());
                                }
                            }
                            for (Code code : Global.repo.codeForClass(c,sigs)) {
                                if (code.method()!=null) {
                                    InlineRepo.put(code.method(),code);
                                    synchronized (numInlineables) {
                                        numInlineables[0]++;
                                    }
                                }
                            }
                        }
                        return true;
                    }
                },
                config.getJobs());
	    InlineRepo.dump("inlineables.txt");
	    InlineRepo.dumpMustInlines("mustInlines.txt");
	    long totalAfter=System.currentTimeMillis();
	    if (Global.verbosity>=1) Global.log.println("processed "+numInlineables[0]+" inlineable methods in "+(totalAfter-totalBefore)+" ms");
	    
	    if (Global.verbosity>=1) Global.log.println("optimizing inlineables...");
	    totalBefore=System.currentTimeMillis();
            InlineRepo.optimizeInlineables();
	    totalAfter=System.currentTimeMillis();
	    if (Global.verbosity>=1) Global.log.println("optimized "+numInlineables[0]+" inlineable methods in "+(totalAfter-totalBefore)+" ms");
	}
	
	if (Global.verbosity>=1) Global.log.println("processing code...");
	final ProcessedCodeWriter pcw=new ProcessedCodeWriter("processed.code",
                                                              Global.repo);
	long totalBefore=System.currentTimeMillis();
        final Iterator< VisibleClass > classIter=
            Global.knownClassesThatMayBeLive().iterator();
        Util.startJoin(
            config.getJobs(),
            new HappyRunnable() {
                public void doStuff() throws Throwable {
                    for (;;) {
                        VisibleClass c=Util.synchronizedSuck(classIter);
                        if (c==null) {
                            break;
                        }
                        if (c.resolved()) {
                            pcw.processAndWrite(c);
                        }
                    }
                }
                public String toString() {
                    return "CodeProcessing";
                }
            });
        pcw.done();
	long totalAfter=System.currentTimeMillis();
        
	// Now that we've emitted types and classes, we have method IDs for
	// flow logging
	if (Settings.FLOW_LOGGING) {
	    final PrintWriter flmapout=Util.wrap(new FileOutputStream("flowLogMap.xml"));
	    try {
		flmapout.println("<FIJIFlowLogMapping>");
                flmapout.println("    <Methods>");
		Global.forAllMethods(new VisibleMethod.Visitor(){
			public void visit(VisibleMethod vm) {
			    flmapout.println("        <Method>");
			    flmapout.println("            <BytecodeSignature>"+
					     Util.escapeXMLData(vm.jniName())+
					     "</BytecodeSignature>");
			    flmapout.println("            <MethodID>"+
					     vm.methodID()+
					     "</MethodID>");
			    flmapout.println("        </Method>");
			}
		    });
                flmapout.println("    </Methods>");
		flmapout.println("    <Types>");
		for (Type t : Global.allResolvedTypesUsedAtRuntime()) {
		    flmapout.println("        <Type>");
		    flmapout.println("            <ShortName>"+
				     Util.escapeXMLData(t.shortName())+
				     "</ShortName>");
		    flmapout.println("            <UniqueID>"+
				     t.uniqueID()+"</UniqueID>");
		    if (t.hasClass() && !t.getClazz().isAbstract()) {
			VisibleField[] fields = t.getClazz().allLaidOutFields();
			if (fields.length != 0) {
			    flmapout.println("            <Fields>");
			    for (VisibleField f : fields) {
				flmapout.println("                <Field>");
				flmapout.println("                    <Name>"+
						 f.getSignature().getName()+
						 "</Name>");
				flmapout.println("                    <Location>"+
						 f.location()+
						 "</Location>");
				flmapout.println("                </Field>");
			    }
			    flmapout.println("            </Fields>");
			}
		    }
		    flmapout.println("        </Type>");
		}
		flmapout.println("    </Types>");

		flmapout.println("</FIJIFlowLogMapping>");
	    } finally {
		flmapout.close();
	    }
	}

	if (Global.verbosity>=1) Global.log.println("processing took "+(totalAfter-totalBefore)+" ms");
	
	int limit=config.getNumLinkablesPerFile();
        
        if (config.getAnalysis().equals("E0CFA")) {
            if (Global.verbosity>=1) Global.log.println("eliminating dead code...");
            totalBefore=System.currentTimeMillis();
            HashSet< VisibleMethod > calledMethods=new HashSet< VisibleMethod >();
            HashSet< VisibleMethod > executedMethods=new HashSet< VisibleMethod >();
            Global.rootsRepo.rootRoots().addCalledMethodsTo(calledMethods);
            Global.rootsRepo.rootRoots().addExecutedMethodsTo(executedMethods);
            // indicate that we consider anonymous methods (i.e. code that is not associated
            // directly with a method, for example native->Java invocation trampolines) to
            // be executed, and thus anything that they execute is also executed.
            executedMethods.add(VisibleMethod.ANONYMOUS);
            // hacks for stuff...
            for (VisibleClass vc : Global.analysis().liveClasses()) {
                if (vc.resolved()) {
                    for (VisibleMethod vm : vc.methods()) {
                        if (vm.isJNI() && vm.shouldHaveNativeGlue()) {
                            executedMethods.add(vm);
                        }
                        if (vm.isExecuted()) {
                            Global.rootsRepo.rootsFor(vm).addCalledMethodsTo(calledMethods);
                            Global.rootsRepo.rootsFor(vm).addExecutedMethodsTo(executedMethods);
                        }
                    }
                }
            }
            CallGraph cg=new CallGraph();
            new CodeReader("processed.code").visit(cg);
            
            // exclude this by default for now ... it takes a lot of time.
            if (false) {
                cg.dump("e0cfaCallGraph");
            }

            if (Global.verbosity>=1) Global.log.println("constraints built, closing...");
            for (;;) {
                int sizes=executedMethods.size()+calledMethods.size();
                Util.transitiveClosure(executedMethods,cg.execEdges(),null);
                HashSet< VisibleMethod > toAdd=new HashSet< VisibleMethod >();
                for (VisibleMethod vm : executedMethods) {
                    calledMethods.add(vm);
                    calledMethods.addAll(cg.callees(vm));
                    for (VisibleClass vc : cg.classesUsed(vm)) {
                        toAdd.addAll(vc.relevantStaticInits());
                    }
                }
                executedMethods.addAll(toAdd);
                if (executedMethods.size()+calledMethods.size()==sizes) {
                    break;
                }
            }
            calledMethods.remove(VisibleMethod.ANONYMOUS);
            executedMethods.remove(VisibleMethod.ANONYMOUS);
            if (Global.verbosity>=1) Global.log.println("found "+calledMethods.size()+"/"+Global.analysis().calledMethods().size()+" methods to be called after processing");
            Global.analysis().pruneCalledMethods(calledMethods);
            Global.analysis().pruneExecutedMethods(executedMethods);
            Global.analysis().dumpCalledMethods("calledMethods2.txt");
            Global.analysis().dumpExecutedMethods("executedMethods2.txt");
            totalAfter=System.currentTimeMillis();
            if (Global.verbosity>=1) Global.log.println("dead code elimination took "+(totalAfter-totalBefore)+" ms");
        
            Global.setAnalysis(Global.analysis()); // hack to inform the context that the analysis has changed
            Global.handleVirtualDispatch();
        }
        
        if (Global.verbosity>=1) Global.log.println("lowering code...");
        final LinkablesWriter lw=new LinkablesWriter(limit){
                protected void write(String codename,
                                     LinkableSet set) throws IOException {
                    if (Global.nativeBackend) {
                        new AsmFileGenerator("compiled code #"+codename,set).generate("gencode"+codename+".S");
                    } else {
                        new CFileGenerator("compiled code #"+codename,set).generate("gencode"+codename+".c");
                    }
                }
            };
        totalBefore=System.currentTimeMillis();
        final int[] cnt=new int[1];
        final Iterator< Code > codeIter=
            new CodeReader("processed.code").iterator();
        Util.startJoin(
            config.getJobs(),
            new HappyRunnable() {
                public void doStuff() throws Throwable {
                    for (;;) {
                        Code c=Util.synchronizedSuck(codeIter);
                        if (c==null) {
                            break;
                        }
                        if (c.method()==null ||
                            c.method().shouldHaveCode()) {
                            if (Global.nativeBackend) {
                                lw.write(ConstifiedAsmLinkable.constify(
                                             CodeLowering.process(c)));
                            } else {
                                lw.write(ConstifiedLinkable.constify(
                                             CodeLowering.process(c)));
                            }
                            synchronized (cnt) {
                                cnt[0]++;
                                if ((cnt[0]%100)==0 &&
                                    Global.verbosity>=1) {
                                    Global.log.println(
                                        "lowered "+cnt[0]+"/"+pcw.count()+
                                        " codes");
                                }
                            }
                        }
                    }
                }
                public String toString() {
                    return "CodeLowering";
                }
            });
        if ((cnt[0]%100)!=0 && Global.verbosity>=1) {
            Global.log.println("lowered "+cnt[0]+"/"+pcw.count()+" codes");
        }
        lw.purge();
        totalAfter=System.currentTimeMillis();
	if (Global.verbosity>=1) Global.log.println("lowering took "+(totalAfter-totalBefore)+" ms");

	if (Global.verbosity>=1) Global.log.println("generating static data");
	
	if (Global.verbosity>=2) StringRepository.printStats();
	
	before=System.currentTimeMillis();
        // this could be parallelized.  but you'd need to do it very sneakily.  I tried
        // the stupid ways and they didn't do anything (except make it run slower).
        // it's possible that the correct solution is to actually not parallelize it but
        // to optimize the serial code.
        new CFileGenerator("classes",ClassRepository.all())
            .generate("genclasses.c");
        new CFileGenerator("strings",StringRepository.all())
            .generate("genstrings.c");
        if (!Global.haveNativeBackend()) {
            new CFileGenerator("upcalls",UpcallMaker.upcalls())
                .generate("genupcalls.c");
        }
        if (StaticProfileCounterRepo.enabled()) {
            new CFileGenerator("profile counters",StaticProfileCounterRepo.allProfileData())
                .generate("genspc.c");
        }
        new CFileGenerator("payload",Payload.payload())
            .generate("genpayload.c");
        new CFileGenerator("types",TypeData.all())
            .generate("gentypes.c");
        // bytecode needs to come after types, because it's lazily populated by the
        // type dumper
        if (Settings.CLASSLOADING) {
            new CFileGenerator("bytecode",BytecodeRepository.all())
                .generate("genbytecode.c");
        }
        new CFileGenerator("debug info",DebugIDRepository.debugTableData())
            .generate("gendebug.c");
        if (Global.indirectGodGivens) {
            new CFileGenerator("god-given indirections",CTypesystemReferences.godGivenIndirections())
                .generate("genindirects.c");
        }
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) Global.log.println("static data generated in "+(after-before)+" ms");
        
        ConfigMapNode typesReport=new ConfigMapNode();
        
        typesReport.put("numBuckets", Type.numBuckets());
        typesReport.put("maxImethodIndex", Global.maxImethodIndex);
        
        ConfigMapNode typesMap=typesReport.putMap("types");
        
        for (Type t : Global.allResolvedTypesUsedAtRuntime()) {
            if (t.isObject()) {
                ConfigMapNode typeReport=typesMap.putMap(t.shortName());
                typeReport.put("display",ConfigListNode.fromArray(t.buckets()));
                typeReport.put("bucket",t.bucket());
                typeReport.put("tid",t.tid());
            
                if (t.hasEffectiveClass()) {
                    VisibleClass vc=t.effectiveClass();
                    if (vc.isInterface()) {
                        ConfigMapNode ifaceMap=typeReport.putMap("ifaceMethods");
                        for (VisibleMethod vm : vc.methods()) {
                            if (vm.isInstance() && !vm.isInitializer() && vm.shouldExist()) {
                                ifaceMap.put(vm.shortName(), vm.getItableIndex());
                            }
                        }
                    } else if (!vc.isInterface() && vc.hasInstances()) {
                        ConfigMapNode itable=typeReport.putMap("itable");
                        for (int i=0;i<vc.itable.length;++i) {
                            if (vc.itable[i]!=null) {
                                itable.put(i,vc.itable[i].shortName());
                            }
                        }
                    }
                }
            }
        }
        
        typesReport.save(new File("typesReport.conf"),
                         "generated by "+Global.fullName());
        
        VerifiabilityRepo.results().save(new File("verifiabilityReport.conf"),
                                         "generated by "+Global.fullName());
        
        ResolutionReport lastReport=Global.resolutionReport();
        lastReport.asConfigNode().save(new File("resolutionReportFinal.conf"));
        lastReport.removeAll(new ResolutionReport(config.getIgnoreErrors()));
        lastReport.removeAll(firstReport);
        lastReport.removeAll(secondReport);
        lastReport.printReport(Global.log);
        
        CTypesystemReferences.asConfigNode().save(new File("finalCTypesystemReferences.conf"),
                                                  "generated by "+Global.fullName());
        
        if (Global.measurePhaseTimings) {
            CodePhaseTimings.dump("codePhaseTimings.txt");
        }

	long fullAfter=System.currentTimeMillis();
	if (Global.verbosity>=1) Global.log.println("bytecode-to-C compilation done in "+(fullAfter-fullBefore)+" ms");
    }
}

