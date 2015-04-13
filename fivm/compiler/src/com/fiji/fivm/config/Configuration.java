/*
 * Configuration.java
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

package com.fiji.fivm.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fiji.config.*;

public class Configuration {
    ConfigMapNode conf;
    
    public Configuration(String filename) {
	try {
            conf=ConfigMapNode.parse(new File(filename));
	} catch (Exception e) {
	    throw new ConfigurationException("Could not load configuration file: "+filename,e);
	}
    }
    
    private String get(String name) {
        return conf.getString(name);
    }
    
    private int getInt(String name) {
        return conf.getInt(name);
    }
    
    private boolean getBool(String name) {
        return conf.getBoolean(name);
    }
    
    private List< String > getStringList(String name) {
        return conf.getList(name).getStrings();
    }
    
    public String getRevision() {
        return get("revision");
    }
    
    public String getBuildType() {
        return get("buildType");
    }
    
    public boolean getOneShotPayload() {
        return getBool("oneShotPayload");
    }
    
    public boolean getHasMain() {
	return getBool("hasMain");
    }
    
    public boolean getStopAfterTypes() {
        return getBool("stopAfterTypes");
    }
    
    public String getMain() {
	return get("main");
    }
    
    public String getOutput() {
	return get("output");
    }
    
    public int getJobs() {
        return getInt("jobs");
    }
    
    public List< String > getFilesForContext(String name) {
        return conf.getMap("contexts").getMap(name).getList("fileList").getStrings();
    }
    
    public List< String > getRootFiles() {
	return getFilesForContext("Root");
    }
    
    public List< String > getAppFiles() {
	return getFilesForContext("App");
    }
    
    public List< String > getFiles() {
        return getStringList("fileList");
    }
    
    public int getVerbosity() {
	return getInt("verbosity");
    }
    
    public int getNumLinkablesPerFile() {
	return getInt("numLinkablesPerFile");
    }
    
    public boolean getPreprocessOnce() {
	return getBool("preprocessOnce");
    }
    
    public boolean getRunSanityCheck() {
	return getBool("runSanityCheck");
    }
    
    public boolean getRunWriteReadTest() {
        return getBool("runWriteReadTest");
    }
    
    public List< String > getNoisyMethods() {
	return getStringList("noisyMethods");
    }
    
    public String getAnalysis() {
	return get("analysis");
    }
    
    public String getObjLocAnalysis() {
	return get("objLocAnalysis");
    }
    
    public String getGC() {
	return get("gc");
    }

    public boolean getGCScopedMemory() {
	return getBool("gcScopedMemory");
    }

    public String getHeaderModel() {
	return get("headerModel");
    }
    
    public String getObjectModel() {
	return get("objectModel");
    }
    
    public int getMaxInlineFixpoint() {
	return getInt("maxInlineFixpoint");
    }
    
    public int getMaxInlineRecurse() {
	return getInt("maxInlineRecurse");
    }
    
    public int getMaxInlineCallerBlocks() {
	return getInt("maxInlineCallerBlocks");
    }
    
    public int getMaxInlineableSize() {
	return getInt("maxInlineableSize");
    }
    
    public int getOMBlocksMax() {
	return getInt("omBlocksMax");
    }
    
    public boolean getDoInline() {
	return getBool("doInline");
    }
    
    public boolean getLateRCE() {
	return getBool("lateRCE");
    }
    
    public boolean getRCE() {
	return getBool("rce");
    }
    
    public int getRCEDistance() {
        return getInt("rceDistance");
    }
    
    public int getNumForwardPasses() {
        return getInt("numForwardPasses");
    }
    
    public boolean getReduceVars() {
	return getBool("reduceVars");
    }
    
    public boolean getNoPollcheck() {
	return getBool("noPollcheck");
    }
    
    public boolean getOptPollcheck() {
        return getBool("optPollcheck");
    }
    
    public int getPollcheckBudget() {
        return getInt("pollcheckBudget");
    }
    
    public int getUnrollBudget() {
        return getInt("unrollBudget");
    }
    
    public int getPeelBudget() {
        return getInt("peelBudget");
    }
    
    public String getNoInlineAttribute() {
	return get("noInlineAttribute");
    }
    
    public boolean getProfileCheckInit() {
	return getBool("profileCheckInit");
    }
    
    public boolean getProfileSubtype() {
	return getBool("profileSubtype");
    }
    
    public boolean getProfileNullCheck() {
	return getBool("profileNullCheck");
    }
    
    public boolean getProfileArrayStore() {
	return getBool("profileArrayStore");
    }
    
    public boolean getProfileArrayBounds() {
	return getBool("profileArrayBounds");
    }
    
    public boolean getProfileCheckDiv() {
	return getBool("profileCheckDiv");
    }
    
    public boolean getProfileInvokes() {
	return getBool("profileInvokes");
    }
    
    public boolean getProfileStackHeight() {
	return getBool("profileStackHeight");
    }
    
    public boolean getCoverage() {
	return getBool("coverage");
    }
    
    public boolean getThrowDebug() {
	return getBool("throwDebug");
    }
    
    public boolean getStaticJNI() {
	return getBool("staticJNI");
    }
    
    public boolean getDynLoading() {
	return getBool("dynLoading");
    }
    
    public boolean getDumbHashCode() {
	return getBool("dumbHashCode");
    }
    
    public String getOSFlavor() {
	return get("osFlavor");
    }
    
    public int getPointerSize() {
	return getInt("pointerSize");
    }
    
    public int getLogPageSize() {
	return getInt("logPageSize");
    }
    
    public int getRTVerbosityLimit() {
        return getInt("rtVerbosityLimit");
    }

    public int getRTFlowLogBufsize() {
	return getInt("rtFlowLogBufsize");
    }
    
    public String getSyncTypeMacroName() {
        return get("syncTypeMacroName");
    }
    
    public String getEndianness() {
	return get("endianness");
    }
    
    public List< String > getArithHelpers() {
	return getStringList("arithHelpers");
    }
    
    public String getVerboseRunMethod() {
	return get("verboseRunMethod");
    }
    
    public ConfigNode getPoundDefines() {
	return conf.getNode("poundDefines");
    }
    
    public boolean getExtractPoundDefines() {
	return getBool("extractPoundDefines");
    }
    
    public boolean getExtractCTypesystemReferences() {
	return getBool("extractCTypesystemReferences");
    }
    
    public String getLibrary() {
	return get("library");
    }
    
    public boolean getDoRefMapOpt() {
        return getBool("doRefMapOpt");
    }
    
    public boolean getBlackStack() {
        return getBool("blackStack");
    }
    
    public boolean getDumpCallGraph() {
        return getBool("dumpCallGraph");
    }
    
    public List< String > getSettings() {
        return getStringList("settings");
    }
    
    public List< String > getPreload() {
        return getStringList("preload");
    }
    
    public List< Reflect > getReflect() {
	ArrayList< Reflect > result=new ArrayList< Reflect >();
        for (ConfigNode cn_ : conf.getList("reflect")) {
            ConfigMapNode cn=cn_.asMap();
            ReflectKind kind;
            String kindStr=cn.getString("kind");
            if (kindStr.equals("called")) {
                kind=ReflectKind.Called;
            } else if (kindStr.equals("dynCalled")) {
                kind=ReflectKind.DynCalled;
            } else if (kindStr.equals("accessed")) {
                kind=ReflectKind.Accessed;
            } else if (kindStr.equals("alloced")) {
                kind=ReflectKind.Alloced;
            } else if (kindStr.equals("used")) {
                kind=ReflectKind.Used;
            } else {
                throw new Error("wrong kind: "+kindStr);
            }
            result.add(new Reflect(kind,
                                   cn.getString("target"),
                                   cn.getString("cause",null)));
            
        }
	return result;
    }
    
    public List< String > getLinkedPayloads() {
        return getStringList("linkedPayloads");
    }
    
    public String getConfigMode() {
        return get("configMode");
    }
    
    public String getFivmcHomeDir() {
        return get("fivmcHomeDir");
    }
    
    public ConfigNode getIgnoreErrors() {
	return conf.getNode("ignoreErrors");
    }
    
    public boolean getHaveCOffsetsAndSizes() {
        return getBool("haveCOffsetsAndSizes");
    }
    
    public ConfigNode getCTypes() {
        return conf.getNode("ctypes");
    }
    
    public boolean getMeasurePhaseTimings() {
        return getBool("measurePhaseTimings");
    }
    
    public boolean getIndirectGodGivens() {
        return getBool("indirectGodGivens");
    }
    
    public boolean getNativeBackend() {
        return getBool("nativeBackend");
    }
    
    public String getRegAlloc() {
        return get("regAlloc");
    }
    
    public boolean getDoRegCoalescing() {
        return getBool("doRegCoalescing");
    }
}
