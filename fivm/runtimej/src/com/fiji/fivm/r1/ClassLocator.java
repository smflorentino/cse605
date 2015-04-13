/*
 * ClassLocator.java
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

package com.fiji.fivm.r1;

import com.fiji.fivm.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.libc.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ClassLocator {
    LinkedList< ClassPathElement > paths=
        new LinkedList< ClassPathElement >();
    
    public ClassLocator() {}
    
    public ClassLocator(String path) {
        addPath(path);
    }
    
    public void add(String pathElement) {
        File f=new File(pathElement).getAbsoluteFile();
        if (f.isDirectory()) {
            paths.add(new DirectoryClassPathElement(f));
        } else {
            try {
                paths.add(new ZipClassPathElement(new ZipFile(f)));
            } catch (IOException e) {
                fivmRuntime.log(ClassLocator.class,0,
                                "Warning: ignoring class path element '"+pathElement+
                                "' because: "+e);
            }
        }
    }
    
    public void addPath(String path) {
        StringTokenizer tox=new StringTokenizer(path,Settings.WIN32?";":":");
        while (tox.hasMoreTokens()) {
            add(tox.nextToken());
        }
    }
    
    public InputStream attemptToLoad(String name) {
        for (ClassPathElement cpe : paths) {
            InputStream inp=cpe.attemptToLoad(name);
            if (inp!=null) {
                return inp;
            }
        }
        return null;
    }
    
    public byte[] attemptToLoadCompletely(String name) {
        InputStream inp=attemptToLoad(name);
        if (inp==null) {
            return null;
        } else {
            try {
                return FileUtils.readCompletely(inp);
            } catch (IOException e) {
                fivmRuntime.log(ClassLocator.class,0,
                                "Got exception while reading "+name+": "+e);
                return null;
            } finally {
                try {
                    inp.close();
                } catch (IOException e) {
                    // omg checked exceptions are retarded.
                    throw new fivmError(e);
                }
            }
        }
    }
    
    public byte[] attemptToLoadClassCompletely(String name) {
        return attemptToLoadCompletely(name.replace('.','/')+".class");
    }
    
    public static final ClassLocator ROOT;
    public static final ClassLocator APP;
    
    static {
        if (Settings.CLASSLOADING) {
            String fileSep;

            if (Settings.WIN32) {
                fileSep="\\";
            } else {
                fileSep="/";
            }

            ROOT=new ClassLocator();
            if (Settings.SEARCH_ROOT_CLASSES_AT_RUNTIME) {
                ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fivmcommon.jar");
                if (Settings.GLIBJ) {
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"cpruntime.jar");
                }
                ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fivmr.jar");
                ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fijirt.jar");
                if (Settings.RTSJ) {
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"cpruntime.jar");
                }
                if (Settings.FIJICORE) {
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fijicore.jar");
                } else if (Settings.GLIBJ) {
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"glibj.zip");
                } else {
                    throw new Error("Unrecognized library.");
                }
                ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fast-md5.jar");
                if (Settings.SCJ) {
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fijiscj.jar");
                    ROOT.add(homeDir()+fileSep+"lib"+fileSep+"scj.jar");
                }
                ROOT.add(homeDir()+fileSep+"lib"+fileSep+"fiji-asm.jar");
            }
            
            APP=new ClassLocator();
            APP.addPath(getenv("CLASSPATH","."));
        } else {
            ROOT=null;
            APP=null;
        }
    }
}

