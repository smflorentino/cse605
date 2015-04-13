/*
 * FCSystemProperties.java
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

package gnu.classpath;

import com.fiji.fivm.r1.*;

import java.util.Properties;

import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.libc.*;

import com.fiji.fivm.Settings;

final class FCSystemProperties {
    @RuntimeImport
    private static native com.fiji.fivm.r1.Pointer fivmr_OS_name();
    
    @RuntimeImport
    private static native com.fiji.fivm.r1.Pointer fivmr_OS_version();
    
    @RuntimeImport
    private static native com.fiji.fivm.r1.Pointer fivmr_OS_arch();
    
    @RuntimeImport
    private static native boolean fivmr_isBigEndian();
    
    static void prePreInit() {
        log(FCSystemProperties.class,1,"Initializing gnu.classpath.SystemProperties...");
    }

    static void preInit(Properties properties) {
        setVMProperties(properties);
        
	properties.setProperty("java.version","1.5");
	properties.setProperty("java.vm.specification.name","Java Virtual Machine Specification");
	properties.setProperty("java.vm.specification.version","1.0");
	properties.setProperty("java.vm.specification.vendor","Sun Microsystems Inc.");
	properties.setProperty("java.specification.version","1.5");
	properties.setProperty("java.specification.vendor","Sun Microsystems Inc.");
	properties.setProperty("java.specification.name","Java Platform API Specification");
	properties.setProperty("java.class.version","50.0");
        if (Settings.WIN32) {
            properties.setProperty("java.io.tmpdir",getenv("TMP","c:\\tmp"));
        } else {
            properties.setProperty("java.io.tmpdir",getenv("TMPDIR","/tmp"));
        }
	properties.setProperty("java.compiler","fivmc");
	properties.setProperty("java.ext.dirs","."); // FIXME
	properties.setProperty("os.name",fromCString(fivmr_OS_name()));
	properties.setProperty("os.version",fromCString(fivmr_OS_version()));
	properties.setProperty("os.arch",fromCString(fivmr_OS_arch()));
        
        String fileSep;
        String pathSep;
        
        if (Settings.WIN32) {
            properties.setProperty("file.separator",fileSep="\\");
            properties.setProperty("path.separator",pathSep=";");
            properties.setProperty("line.separator","\r\n");
            properties.setProperty("user.home",getenv("USERPROFILE","c:\\"));
            properties.setProperty("user.name",getenv("USERNAME","Administrator"));
            properties.setProperty("user.dir",getcwd());
        } else {
            properties.setProperty("file.separator",fileSep="/");
            properties.setProperty("path.separator",pathSep=":");
            properties.setProperty("line.separator","\n");
            if (Settings.RTEMS) {
                properties.setProperty("user.name","root");
                properties.setProperty("user.home","/");
                properties.setProperty("user.dir","/");
            } else {
                properties.setProperty("user.name",getenv("USER","root"));
                properties.setProperty("user.home",getenv("HOME","/"));
                properties.setProperty("user.dir",getcwd());
            }
        }

	properties.setProperty("java.class.path",getenv("CLASSPATH","."));

	properties.setProperty("gnu.cpu.endian",fivmr_isBigEndian()?"big":"little");
	properties.setProperty("java.library.path",
                               getenv("JAVA_JNI_PATH",".")+pathSep+
                               homeDir()+pathSep+
                               homeDir()+fileSep+"lib"+pathSep+
                               homeDir()+fileSep+"lib"+fileSep+"glibj.zip");
    }
    
    static void postInit(Properties properties) {
        setArgumentProperties(properties);
    }
}

