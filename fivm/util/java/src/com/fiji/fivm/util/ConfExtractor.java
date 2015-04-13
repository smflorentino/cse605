/*
 * ConfExtractor.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.rt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;
import com.fiji.config.*;

public class ConfExtractor {
    static ConfigMapNode conf;
    
    static void putGCProps(MemoryManager.GarbageCollector gc) {
        conf.put(
            "gcParams",
            new ConfigMapNode(
                "defTrigger",gc.getDefaultTrigger(),
                "defMaxMem",gc.getDefaultMaxMemory(),
                "threadPriority", gc.getThreadPriority()));
    }
    
    public static void main(String[] v) throws Throwable {
	com.fiji.fivm.config.Configuration inConf=
	    new com.fiji.fivm.config.Configuration(v[0]);
        
	Global.verbosity=inConf.getVerbosity();
	
	Configuration config;
	if (inConf.getOSFlavor().equals("RTEMS")) {
	    config=fivmcSupport.newRTOSConfiguration(1<<inConf.getLogPageSize(),false);
	} else {
	    config=fivmcSupport.newConfiguration(1<<inConf.getLogPageSize(),false);
	}

	assert inConf.getHasMain();

        if (inConf.getConfigMode().equals("java")) {
            try {
                // FIXME: this code needs to be made a heck of a lot more rugged...
            
                LinkedList< URL > urls=new LinkedList< URL >();
                LinkedList< String > classFiles=new LinkedList< String >();
                for (String flnm : inConf.getFiles()) {
                    File file=new File(flnm);
                    if (file.isDirectory()) {
                        // this should never happen, but we handle it anyway...
                        if (flnm.endsWith(File.separator)) {
                            urls.add(file.toURL());
                        } else {
                            urls.add(new File(flnm+File.separator).toURL());
                        }
                    } else if (Util.isJarOrZip(flnm)) {
                        urls.add(file.toURL());
                    } else if (Util.isClassFile(flnm)) {
                        classFiles.add(flnm);
                    }
                }
	
                if (!classFiles.isEmpty()) {
                    final File jarFile=File.createTempFile("fivmcConfExtractor",".jar");
                    if (true) java.lang.Runtime.getRuntime().addShutdownHook(
                        new Thread() {
                            public void run() {
                                try {
                                    jarFile.delete();
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    JarOutputStream jout=new JarOutputStream(
                        new FileOutputStream(jarFile));
                    try {
                        for (String flnm : classFiles) {
                            byte[] bytecode=Util.readCompletely(flnm);
                            String newName=Util.getClassName(bytecode)+".class";
                            if (Global.verbosity>=1) {
                                Global.log.println("Remapping "+flnm+" to "+newName);
                            }
                            jout.putNextEntry(new JarEntry(newName));
                            jout.write(bytecode);
                            jout.closeEntry();
                        }
                    } finally {
                        jout.close();
                    }
                    urls.add(jarFile.toURL());
                }
	
                if (Global.verbosity>=1) {
                    Global.log.println("URLS with code: "+urls);
                }
	
                URLClassLoader ucl=new URLClassLoader(urls.toArray(new URL[0]));
                Class<?> configClass=null;
	
                try {
                    configClass=ucl.loadClass(inConf.getMain().replace('/','.')+"$Config");
                } catch (ClassNotFoundException e) {
                    if (Global.verbosity>=1) {
                        Global.log.println(
                            "Warning: Could not find configuration class: "+inConf.getMain()+"$Config");
                    }
                }
	
                Method confMethod=null;
                if (configClass!=null) {
                    try {
                        confMethod=configClass.getMethod("configure",
                                                         Configuration.class);
                    } catch (NoSuchMethodException e) {
                        if (Global.verbosity>=1) {
                            Global.log.println(
                                "Could not find configuration method in "+
                                inConf.getMain()+"$Config; was looking for "+
                                "configure(Lcom/fiji/rt/Configuration)V");
                        }
                    }
                }
	
                if (confMethod!=null) {
                    confMethod.invoke(null,config);
                }
            } catch (Throwable e) {
                if (Global.verbosity>=1) {
                    e.printStackTrace(Global.log);
                }
                Global.log.println("Could not extract configuration from code: "+e);
            }
        } else if (inConf.getConfigMode().equals("none")) {
            // ok
        } else {
            throw new Error("unrecognized configuration mode: "+inConf.getConfigMode());
        }
        
        conf=new ConfigMapNode();
        
        conf.put("maxThreads",config.getMaxThreads());
        conf.put("saSize",config.getStackAllocSize());
        conf.put("gcScopedMemory",config.getGCScopedMemory());
	
        MemoryManager mm=config.getMemoryManager();
        if (mm instanceof MemoryManager.Immortal) {
            conf.put("gc","NONE");
        } else if (mm instanceof MemoryManager.ConcurrentMarkRegion) {
            conf.put("gc","CMR");
        } else if (mm instanceof MemoryManager.HybridFragmenting) {
            conf.put("gc","HF");
        } else {
            throw new Error();
        }
        if (mm instanceof MemoryManager.GarbageCollector) {
            MemoryManager.GarbageCollector gc=
                (MemoryManager.GarbageCollector)mm;
            putGCProps(gc);
        }

        // FIXME ... implement more
	
        if (!conf.has("gcParams")) {
            // HACK - make sure that if the user set mm to Immortal, we still
            // have put in the defaults for GC
            putGCProps((MemoryManager.GarbageCollector)
                       fivmcSupport.newConfiguration(
                           1<<inConf.getLogPageSize(),false).getMemoryManager());
        }
        
        if (config instanceof RTOSConfiguration) {
            RTOSConfiguration rtosc=(RTOSConfiguration)config;
            conf.put(
                "rtosConfig",
                new ConfigMapNode(
                    "interruptStackSize",rtosc.getInterruptStackSize(),
                    "threadStackSize",rtosc.getThreadStackSize(),
                    "nanosPerTick",rtosc.getNanosPerTick(),
                    "ticksPerTimeslice",rtosc.getTicksPerTimeslice(),
                    "maxOSThreads",rtosc.getMaxOSThreads(),
                    "maxFileDescriptors",rtosc.getMaxFileDescriptors()));
        }
        
        conf.save(new File("jconf.conf"),
                  "generated by com.fiji.fivm.util.ConfExtractor version "+Config.VERSION);
    }
}


