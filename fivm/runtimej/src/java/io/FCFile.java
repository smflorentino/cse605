/*
 * FCFile.java
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

package java.io;

import com.fiji.fivm.r1.*;
import java.util.LinkedList;

final class FCFile {

    static boolean canRead(String path) {
	return libc.access(path,CVar.getInt("R_OK"))==0;
    }

    static boolean exists(String path) {
	return libc.access(path,CVar.getInt("F_OK"))==0;
    }
    
    static boolean isDirectory(String path) {
        int res=libc.statGetMode(path);
	return res>=0 && (res&CVar.getInt("S_IFDIR"))!=0;
    }
    
    static boolean canWrite(String path) {
	return libc.access(path,CVar.getInt("W_OK"))==0;
    }
    
    static boolean canExecute(String path) {
	return libc.access(path,CVar.getInt("X_OK"))==0;
    }
    
    static boolean create(String path) throws IOException {
	int res=libc.open(path,
			  (CVar.getInt("O_WRONLY")|
			   CVar.getInt("O_CREAT")|
			   CVar.getInt("O_EXCL")),
			  0666);
	
	if (res<0 && libc.errno()==CVar.getInt("EEXIST")) {
	    return false;
	}
	
	libc.checkIO(libc.close(libc.checkIO(res)));
	
	return true;
    }
    
    static boolean delete(String path) {
	return libc.unlink(path)==0;
    }
    
    static String getAbsolutePath(String path) {
	return libc.getcwd()+"/"+path;
    }
    
    @StackAllocation
    static String toCanonicalForm(String path) throws IOException {
	Pointer pathCstr=fivmRuntime.getCStringFullStack(path);
	Pointer result=Magic.addressOfElement(
	    new byte[CVar.getInt("PATH_MAX")],0);
	if (libc.realpath(pathCstr,result)==Pointer.zero()) {
	    libc.throwIOException();
	}
	return fivmRuntime.fromCStringFullInHeap(result);
    }
    
    static String getName(String path) {
	int lastSlash=path.lastIndexOf('/');
	if (lastSlash<0) {
	    return path;
	} else {
	    return path.substring(lastSlash+1,path.length());
	}
    }
    
    static boolean isAbsolute(String path) {
	return path.startsWith("/");
    }
    
    static boolean isFile(String path) {
        int res=libc.statGetMode(path);
	return res>=0 && (res&CVar.getInt("S_IFREG"))!=0;
    }
    
    static boolean isHidden(String path) {
	return getName(path).startsWith(".");
    }
    
    @StackAllocation
    static long lastModified(String path) {
	Pointer statBuf=Magic.addressOfElement(
	    new byte[CType.sizeof("struct stat").castToInt()],0);
	if (libc.stat(path,statBuf)!=0) {
	    return 0l;
	} else {
	    if (CMacro.defined("HAVE_STAT_MTIMESPEC")) {
		Pointer st_mtimespec=statBuf.add(CType.offsetof("struct stat","st_mtimespec"));
		long nanos;
		nanos=CType.getInt(st_mtimespec,"struct timespec","tv_sec");
		nanos*=1000l;
		nanos*=1000l;
		nanos*=1000l;
		nanos+=CType.getInt(st_mtimespec,"struct timespec","tv_nsec");
		return nanos/1000l/1000l;
	    } else if (CMacro.defined("HAVE_STAT_MTIME")) {
		return CType.getInt(statBuf,"struct stat","st_mtime");
	    } else {
		throw new fivmError("Don't know how to parse struct stat");
	    }
	}
    }
    
    @StackAllocation
    static long length(String path) {
	Pointer statBuf=Magic.addressOfElement(
	    new byte[CType.sizeof("struct stat").castToInt()],0);
	if (libc.stat(path,statBuf)!=0) {
	    return 0l;
	} else {
	    return CType.getLong(statBuf,"struct stat","st_size");
	}
    }
    
    @StackAllocation
    static String[] list(String path) {
	Pointer dirp=libc.opendir(path);
	if (dirp==Pointer.zero()) {
	    return null;
	}
	try {
	    Pointer entry=Magic.addressOfElement(
		new byte[CType.sizeof("struct dirent").castToInt()],0);
	    Pointer entryPtr=Magic.addressOfElement(new Pointer[1],0);
	    
	    LinkedList< String > result=new LinkedList< String >();
	    for (;;) {
		if (libc.readdir_r(dirp,entry,entryPtr)!=0) {
		    return null;
		}
		if (entryPtr.loadPointer()==Pointer.zero()) {
		    break;
		}
		String name=fivmRuntime.fromCStringFull(
		    CType.getPointer(entry,"struct dirent","d_name"));
		if (name.equals(".") || name.equals("..")) {
		    continue;
		}
		result.add(name);
	    }
	    
	    return result.toArray(new String[0]);
	} finally {
	    libc.closedir(dirp);
	}
    }
    
    static boolean mkdir(String path) {
	return libc.mkdir(path,0777)==0;
    }
    
    private static int modeify(int mode,int bit,boolean value,boolean ownerOnly) {
	mode&=07777;
	int operator=bit;
	if (ownerOnly) {
	    operator<<=6;
	} else {
	    operator=
		(operator<<0) |
		(operator<<3) |
		(operator<<6);
	}
	if (value) {
	    mode|=operator;
	} else {
	    mode&=~operator;
	}
	return mode;
    }
    
    private static boolean chmodHelper(String path,int bit,boolean value,boolean ownerOnly) {
	int mode=libc.statGetMode(path);
	if (mode<0) return false;
	return libc.chmod(path,modeify(mode,bit,value,ownerOnly))==0;
    }
    
    static boolean setReadable(String path,boolean readable,boolean ownerOnly) {
	return chmodHelper(path,04,readable,ownerOnly);
    }
    
    static boolean setWritable(String path,boolean writable,boolean ownerOnly) {
	return chmodHelper(path,02,writable,ownerOnly);
    }
    
    static boolean setExecutable(String path,boolean executable,boolean ownerOnly) {
	return chmodHelper(path,01,executable,ownerOnly);
    }
    
    @StackAllocation
    static long getTotalSpace(String path) {
	Pointer statfs=libc.statfs(path);
	return (long)CType.getShort(statfs,"struct statfs","f_bsize")
	    * (long)CType.getInt(statfs,"struct statfs","f_blocks");
    }
    
    @StackAllocation
    static long getFreeSpace(String path) {
	Pointer statfs=libc.statfs(path);
	return (long)CType.getShort(statfs,"struct statfs","f_bsize")
	    * (long)CType.getInt(statfs,"struct statfs","f_bfree");
    }
    
    @StackAllocation
    static long getUsableSpace(String path) {
	Pointer statfs=libc.statfs(path);
	return (long)CType.getShort(statfs,"struct statfs","f_bsize")
	    * (long)CType.getInt(statfs,"struct statfs","f_bavail");
    }
    
    static File[] listRoots() {
	return new File[]{new File("/")};
    }
    
    static boolean renameTo(String from,String to) {
	return libc.rename(from,to)==0;
    }
    
    static boolean setLastModified(String path,long time) {
	// FIXME
	return false;
    }
    
    static boolean isCaseSensitive() {
	if (CMacro.defined("HAVE_CASE_INSENSITIVE_FS")) {
	    return false;
	} else {
	    return true;
	}
    }
}
