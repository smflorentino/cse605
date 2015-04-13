/*
 * Util.java
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
import java.math.*;
import java.io.*;
import java.nio.*;
import java.lang.reflect.*;
import com.fiji.asm.ClassReader;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;
import com.fiji.fivm.Constants;
import com.fiji.fivm.Settings;
import com.fiji.fivm.FileUtils;
import com.fiji.util.MyStack;
import com.twmacinta.util.MD5;

public class Util {
    private Util() {}
    
    public static < T > void retain(Set< T > target,
                                    Set< T > source) {
        for (Iterator< T > i=target.iterator();
             i.hasNext();) {
            if (!source.contains(i.next())) {
                i.remove();
            }
        }
    }
    
    public static < T, U > HashMap< T, U > copy(Map< T, U > old) {
	HashMap< T, U > result=new HashMap< T, U >();
	if (old!=null) {
	    result.putAll(old);
	}
	return result;
    }
    
    public static < T, U > HashMap< T, U > copy(Map< T, U > old, Iterable< T > include) {
	HashMap< T, U > result=new HashMap< T, U >();
	for (T key : include) {
	    result.put(key,old.get(key));
	}
	return result;
    }
    
    public static < T > HashSet< T > copy(Set< T > old) {
	HashSet< T > result=new HashSet< T >();
	addAll(result,old);
	return result;
    }
    
    public static < T > boolean addAll(Set< T > to, Set< T > from) {
	if (from!=null) {
	    return to.addAll(from);
	}
	return false;
    }
    
    public static < T > HashSet< T > union(Set< T > a, Set< T > b) {
	HashSet< T > result=Util.copy(a);
	addAll(result,b);
	return result;
    }
    
    public static < T, U > boolean removeAll(HashMap< T, U > map, Collection< T > keys) {
	boolean changed=false;
	for (T key : keys) {
	    if (map.remove(key)!=null) changed=true;
	}
	return changed;
    }
    
    public static < T > ArrayList< T > toArray(Iterable< T > i) {
	ArrayList< T > result=new ArrayList< T >();
	for (T x : i) {
	    result.add(x);
	}
	return result;
    }
    
    public static < T, U > void oneWayDiff(HashMap< T, U > one,
                                           HashMap< T, U > two,
                                           HashSet< T > diff) {
        for (T key : one.keySet()) {
            if (!two.containsKey(key) ||
                !one.get(key).equals(two.get(key))) {
                diff.add(key);
            }
        }
    }
    
    public static < T, U > HashSet< T > diff(HashMap< T, U > one,
                                             HashMap< T, U > two) {
        HashSet< T > diff=new HashSet< T >();
        oneWayDiff(one,two,diff);
        oneWayDiff(two,one,diff);
        return diff;
    }
    
    public static < T > Iterable< T >
    composeIterables(Iterable< ? extends T > a,Iterable< ? extends T > b) {
	final Iterator< ? extends T > ai=a.iterator();
	final Iterator< ? extends T > bi=b.iterator();
	return new Iterable< T >() {
	    public Iterator< T > iterator() {
		return new Iterator< T >() {
		    public boolean hasNext() {
			return ai.hasNext() || bi.hasNext();
		    }
		    public T next() {
			if (ai.hasNext()) {
			    return ai.next();
			} else {
			    return bi.next();
			}
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public static < T > Iterable< T >
    pushIterable(final T value, final Iterable< T > i) {
	return new Iterable< T >() {
	    public Iterator< T > iterator() {
		return new Iterator< T >() {
		    boolean first=true;
		    Iterator< T > ii=i.iterator();
		    public boolean hasNext() {
			return first || ii.hasNext();
		    }
		    public T next() {
			if (first) {
			    first=false;
			    return value;
			} else {
			    return ii.next();
			}
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public static < T > Iterable< T > singleIterable(final T value) {
	return new Iterable< T >() {
	    public Iterator< T > iterator() {
		return new Iterator< T >() {
		    boolean done=false;
		    public boolean hasNext() {
			return !done;
		    }
		    public T next() {
			if (done) {
			    throw new NoSuchElementException();
			} else {
			    done=true;
			    return value;
			}
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public static String dump(Object[] array,int upTo) {
	StringBuffer buf=new StringBuffer();
	for (int i=0;i<upTo;++i) {
	    if (i!=0) {
		buf.append(", ");
	    }
	    buf.append(array[i]);
	}
	return buf.toString();
    }
    
    public static String dump(Object[] array) {
	return dump(array,array.length);
    }
    
    public static <T> String dump(Iterable< T > i) {
	StringBuffer buf=new StringBuffer();
	boolean first=true;
	for (Object o : i) {
	    if (first) {
		first=false;
	    } else {
		buf.append(", ");
	    }
	    buf.append(o);
	}
	return buf.toString();
    }
    
    public static String dump(String str) {
	StringBuilder buf=new StringBuilder();
	for (int i=0;i<str.length();++i) {
	    char c=str.charAt(i);
	    if (c>=32 && c<=126 && c!='<' && c!='>') {
		buf.append(c);
	    } else {
		buf.append("<");
		buf.append((int)c);
		buf.append(">");
	    }
	}
	return buf.toString();
    }
    
    public static < T > HashSet< T > makeSet(T x) {
	HashSet< T > result=new HashSet< T >();
	result.add(x);
	return result;
    }
    
    public static < T > ArrayList< T > makeArray(T x) {
	ArrayList< T > result=new ArrayList< T >();
	result.add(x);
	return result;
    }
    
    public static int hashCode(Object[] a) {
	int result=a.length;
	for (Object o : a) {
	    result+=o.hashCode();
	}
	return result;
    }
    
    public static boolean equals(Object[] a,Object[] b) {
	if (a==b) return true;
	if (a.length!=b.length) return false;
	for (int i=0;i<a.length;++i) {
	    if (a[i]!=b[i]) return false;
	}
	return true;
    }
    
    public static <T> boolean intersects(Set< T > a,Set< T > b) {
	if (b.size()<a.size()) {
	    Set< T > tmp=a;
	    a=b;
	    b=tmp;
	}
	for (T x : a) {
	    if (b.contains(x)) return true;
	}
	return false;
    }
    
    public static byte[] toUTF8(String str) {
	try {
	    return str.getBytes("UTF-8");
	} catch (UnsupportedEncodingException e) {
	    throw new Error(e);
	}
    }
    
    public static PrintWriter wrap(OutputStream o,boolean autoflush) {
	try {
	    return new PrintWriter(new OutputStreamWriter(o,"UTF-8"),autoflush);
	} catch (UnsupportedEncodingException e) {
	    throw new Error(e);
	}
    }
	    
    public static PrintWriter wrap(OutputStream o) {
	return wrap(o,false);
    }
    
    public static PrintWriter wrapAutoflush(OutputStream o) {
	return wrap(o,true);
    }
    
    static String validChars=
	"1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    static BigInteger validCharsLen=
	BigInteger.valueOf(validChars.length());
    
    /** Returns an MD5 hash of the given string, expressed as a variable-length
	sequence of digits and latin lower-case and upper-case letters. */
    // this is 2% of my exec time
    public static String hash(String s) {
        BigInteger bi=new BigInteger(
            1,MD5.digest(toUTF8(s)));
        if (Global.verbosity>=3) {
            Global.log.println("Hash for "+s+" is: "+bi);
        }
        StringBuffer result=new StringBuffer();
        while (bi.compareTo(BigInteger.ZERO)>0) {
            BigInteger[] dar=bi.divideAndRemainder(validCharsLen);
            result.append(validChars.charAt(dar[1].intValue()));
            bi=dar[0];
        }
        if (Global.verbosity>=3) {
            Global.log.println("Returning mangled hash for "+s+": "+result);
        }
        return result.toString();
    }
    
    public static Error rethrow(Throwable e) {
	if (e!=null) {
	    if (e instanceof RuntimeException) {
		throw (RuntimeException)e;
	    } else if (e instanceof Error) {
		throw (Error)e;
	    } else {
		throw new Error(e);
	    }
	}
        return null;
    }
    
    public static String hidePunct(String s) {
	StringBuffer result=new StringBuffer();
	for (int i=0;i<s.length();++i) {
	    char c=s.charAt(i);
	    if (c>='a' && c<='z' ||
		c>='A' && c<='Z' ||
		c>='0' && c<='9') {
		result.append(c);
	    } else {
		result.append('_');
	    }
	}
	return result.toString();
    }
    
    public static Arg[] produceZero(Code code,
				    Type type,
				    Operation before) {
	if (type==Type.VOID) {
	    return Arg.EMPTY;
	} else {
            return new Arg[]{ type.effectiveBasetype().makeZero() };
	}
    }
    
    public static ArrayList< Code > deepCopyAsArray(Iterable< Code > list) {
	ArrayList< Code > result=new ArrayList< Code >();
	for (Code c : list) {
	    result.add(c.copy());
	}
	return result;
    }
    
    public static int readCompletely(InputStream in,byte[] data)
        throws IOException {
        return FileUtils.readCompletely(in,data);
    }
    
    public static byte[] readCompletely(InputStream in) throws IOException {
        return FileUtils.readCompletely(in);
    }
    
    public static byte[] readCompletely(String flnm) throws IOException {
        return FileUtils.readCompletely(flnm);
    }

    public static boolean hasMagic(String filename,byte[] magic)
	throws IOException {
	FileInputStream fin=new FileInputStream(filename);
	try {
	    byte[] test=new byte[magic.length];
	    if (readCompletely(fin,test)!=magic.length) {
		return false;
	    }
	    for (int i=0;i<magic.length;++i) {
		if (test[i]!=magic[i]) {
		    return false;
		}
	    }
	    return true;
	} finally {
	    fin.close();
	}
    }
    
    public static ArrayList< Code > codeForClass(VisibleClass c,
						 Set< MethodSignature > methods) {
	if (methods!=null && methods.isEmpty()) return Code.EMPTY_AL;
	if (Global.verbosity>=2) {
	    Global.log.println("preprocessing "+c);
	}
	long before=System.currentTimeMillis();
	ArrayList< Code > l=CodeParser.parseMethods(c,methods);
	CodePreprocessor.process(l);
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("preprocessed "+l.size()+" methods in "+c+" in "+(after-before)+" ms");
	}
	return l;
    }
    
    public static String getClassName(byte[] bytecode) {
	final String[] className=new String[1];
	new ClassReader(bytecode).accept(
	    new EmptyVisitor(){
		public void visit(int version,
				  int access,
				  UTF8Sequence name,
				  UTF8Sequence signature,
				  UTF8Sequence superName,
				  UTF8Sequence[] interfaces) {
		    className[0]=name.toString();
		}
	    },
	    ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
	return className[0];
    }
    
    public static String getClassName(String filename) throws IOException {
	return getClassName(readCompletely(filename));
    }
    
    public static int lastSetBit(BitSet set) {
	for (int i=set.size()-1;i>=0;--i) {
	    if (set.get(i)) {
		return i;
	    }
	}
	return -1;
    }
    
    public static int bitSetWord(BitSet set,int low,int high) {
	assert high-low+1<=32;
	int result=0;
	for (int i=0;i<high-low+1;++i) {
	    if (set.get(i+low)) {
		result|=(1<<i);
	    }
	}
	return result;
    }
    
    public static int bitSetWord(BitSet set) {
	int high=lastSetBit(set);
	assert high<=31;
	return bitSetWord(set,0,high);
    }
    
    public static void bitSetWords(ArrayList< ? super Integer > target,
				   BitSet set,
				   int low,
				   int high) {
	for (int i=0;i<high-low+1;i+=32) {
	    target.add(bitSetWord(set,i+low,i+low+31));
	}
    }
    
    public static void bitSetWords(ArrayList< ? super Integer > target,
				   BitSet set) {
	bitSetWords(target,set,0,lastSetBit(set));
    }
    
    public static int numBitSetWords(BitSet set) {
	return (lastSetBit(set)+1+31)/32;
    }

    public static String cStringEscape(String str) {
	StringBuilder buf=new StringBuilder();
	for (byte b : toUTF8(str)) {
	    if ((b>='a' && b<='z') ||
		(b>='A' && b<='Z') ||
		(b>='0' && b<='9') ||
		b=='_' || b=='-' || b=='.' ||
		b=='/' || b==';' || b=='$' ||
                b==' ' || b=='+' /* add more cases */) {
		buf.append((char)b);
	    } else {
		buf.append("\\"+
			   (""+((b/64)%8))+
			   (""+((b/8)%8))+
			   (""+(b%8)));
	    }
	}
	return buf.toString();
    }
    
    public static String uniqueifyName(String name,
				       Set< String > taken) {
	while (taken.contains(name)) {
            int offset=name.length();
            for (;;) {
                if (offset>0 && Character.isDigit(name.charAt(offset-1))) {
                    offset--;
                } else {
                    break;
                }
            }
            if (offset==name.length()) {
                name+="2";
            } else {
                name=name.substring(0,offset)+(Integer.parseInt(name.substring(offset,name.length()))+1);
            }
	}
	return name;
    }
    
    public static void dump(String filename,Collection<?> list) throws IOException {
	PrintWriter fout=Util.wrap(new FileOutputStream(filename));
	try {
	    for (Object o : list) {
		fout.println(o.toString());
	    }
	} finally {
	    fout.close();
	}
    }
    
    public static < T extends Comparable< ? super T >>
    void dumpSorted(String filename,Collection< T > list_) throws IOException {
	ArrayList< T > list=new ArrayList< T >(list_);
	Collections.sort(list);
	dump(filename,list);
    }
    
    public static < T extends JNINameable >
    ArrayList< T > sortJNI(Collection< T > list) {
	ArrayList< T > result=
	    new ArrayList< T >(list);
	Collections.sort(
	    result,
	    new Comparator< T >() {
		public int compare(T a,T b) {
		    return a.jniName().compareTo(b.jniName());
		}
	    });
	return result;
    }
    
    public static void dumpSortedJNI(String filename,Collection< ? extends JNINameable > list)
	throws IOException {
	ArrayList< String > names=new ArrayList< String >();
	for (JNINameable m : list) {
	    names.add(m.jniName());
	}
	dumpSorted(filename,names);
    }
    
    public static String zeroPad(int x,int numCells) {
	String result=""+x;
	while (result.length()<numCells) {
	    result="0"+result;
	}
	return result;
    }
    
    public static String zeroPad(int x) {
	return zeroPad(x,4);
    }
    
    public static String upcaseFirst(String s) {
	if (s.length()>0) {
	    return ""+Character.toUpperCase(s.charAt(0))+
		s.substring(1,s.length());
	} else {
	    return "";
	}
    }
    
    public static String downcaseFirst(String s) {
	if (s.length()>0) {
	    return ""+Character.toLowerCase(s.charAt(0))+
		s.substring(1,s.length());
	} else {
	    return "";
	}
    }
    
    public static String extractString(Header h,Arg str) {
	Instruction stringInst=h.code().getAssigns().assignFor(str);
	if (stringInst==null || stringInst.opcode()!=OpCode.GetString) {
	    throw new CompilerException("Cannot extract string for "+str);
	}
	return ((GetStringInst)stringInst).value();
    }

    public static VisibleField extractInstField(Header h,Arg object,Arg name) {
	if (!object.type().hasClass()) {
	    throw new CompilerException("Cannot compile field intrinsic on non-class receiver");
	}
	VisibleField f=
	    h.code.getContext().resolveFieldByName(
		h.code.getOwner(),
		object.type().getClazz(),
		extractString(h,name));
	if (!f.isInstance()) {
	    throw new ResolutionFailed(
                object.type().getClazz(),
                new ResolutionID(object.type().getClazz(),"instance field "+f.jniName()),
                "Expected an instance field but got a static field: "+f.jniName());
	}
	return f;
    }
    
    public static Type extractType(Header h,Arg clazz) {
	Instruction classInst=h.code().getAssigns().assignFor(clazz);
	if (classInst==null || classInst.opcode()!=OpCode.GetType) {
	    throw new CompilerException("Cannot extract class in static field intrinsic");
	}
	return ((TypeInst)classInst).getType();
    }
    
    public static VisibleClass extractClass(Header h,Arg clazz) {
	Type t=extractType(h,clazz);
	if (!t.hasClass()) {
	    throw new CompilerException("Cannot compile field intrinsic on non-class receiver");
	}
	return t.getClazz();
    }
    
    public static VisibleField extractFieldStatic(Header h,Arg clazz,Arg name) {
	return h.code.getContext().resolveFieldByName(h.code.getOwner(),
						      extractClass(h,clazz),
						      extractString(h,name));
    }
    
    public static VisibleField extractInstFieldStatic(Header h,Arg clazz,Arg name) {
	VisibleField f=extractFieldStatic(h,clazz,name);
	if (!f.isInstance()) {
	    throw new ResolutionFailed(
                extractClass(h,clazz),
                new ResolutionID(extractClass(h,clazz),"instance field "+f.jniName()),
                "Expected an instance field but got a static field: "+f.jniName());
	}
	return f;
    }
    
    public static VisibleField extractStaticField(Header h,Arg clazz,Arg name) {
	VisibleField f=extractFieldStatic(h,clazz,name);
	if (!f.isStatic()) {
	    throw new ResolutionFailed(
                extractClass(h,clazz),
                new ResolutionID(extractClass(h,clazz),"static field "+f.jniName()),
                "Expected a static field but not an instance field: "+f.jniName());
	}
	return f;
    }
    
    public static long align(long value,long align) {
	return (value+align-1)&~(align-1);
    }
    
    public static boolean isJarOrZip(String filename) throws IOException {
	return hasMagic(filename,new byte[]{'P','K'});
    }
    
    public static boolean isClassFile(String filename) throws IOException {
	return Util.hasMagic(filename,new byte[]{(byte)0xCA,
						 (byte)0xFE,
						 (byte)0xBA,
						 (byte)0xBE});
    }
    
    public static int countExisting(Iterable< ? extends VisibleMember > c) {
	int result=0;
	for (VisibleMember m : c) {
	    if (m.shouldExist()) {
		result++;
	    }
	}
	return result;
    }
    
    public static Arg putCast(Code code,Operation before,Type t,Arg value) {
	Var result=code.addVar(t.asExectype());
	before.prepend(
	    new TypeInst(
		before.di(),OpCode.Cast,
		result,new Arg[]{value},
		t));
	return result;
    }
    
    public static Arg pointerifyObject(Code code,Operation before,Arg value) {
	if (value.type().isObject()) {
            Var result=code.addVar(Exectype.POINTER,
                                   value.type());
            before.prepend(
                new TypeInst(
                    before.di(),OpCode.Cast,
                    result,new Arg[]{value},
                    Type.POINTER));
            return result;
	} else {
	    return value;
	}
    }

    public static void buildHeader(ArrayList< Object > data,
                                   Type t) {
	if (Global.gc.hasCMHeader()) {
	    data.add(new Pointer(
			 ((long)Constants.CMR_GC_ALWAYS_MARKED)
			 << (Global.pointerSize*8-2)));
	} else {
	    data.add(new Pointer((long)-1));
	}
	if (Global.hm==HeaderModel.POISONED) {
	    data.add(
		new TaggedLink(
		    TypeData.forType(t),
		    1));
	} else {
	    data.add(
		TypeData.forType(t));
	}
    }
    
    /**
     * Gives the size of a constant suitable for LocalDataConstant
     */
    public static int sizeof(Object o) {
        if (o instanceof Byte) {
            return 1;
        } else if (o instanceof Character ||
                   o instanceof Short) {
            return 2;
        } else if (o instanceof Integer) {
            return 4;
        } else if (o instanceof Long) {
            return 8;
        } else if (o instanceof Padding) {
            return ((Padding)o).size();
        } else if (o instanceof Pointerable) {
            return Global.pointerSize;
        } else {
            throw new Error("invalid object: "+o);
        }
    }
    
    public static int sizeof(List< Object > list) {
        int size=0;
        for (Object o : list) {
            size+=sizeof(o);
        }
        return size;
    }
    
    // FIXME broken for large-alignment arraylets
    public static Pointerable buildArraylet(String name,
                                            List< Object > data,
                                            Type type) {
        ArrayList< Object > arrData=new ArrayList< Object >();
        
        arrData.add(
            new DisplacedLink(
                new RemoteDataConstant(name),
                FragmentedObjectRepresentation.arrayHeaderSize));
        
        Util.buildHeader(arrData,type);
        
        arrData.add(new Integer(data.size()));
        
        arrData.addAll(data);
        
        return new LocalDataConstant(name,arrData);
    }
    
    public static ArrayList< ArrayList< Object > >
    buildFragmentedDatas(List< Object > data) {
        ArrayList< ArrayList< Object > > datas=new ArrayList< ArrayList< Object > >();
        int sizeCnt=
            FragmentedObjectRepresentation.chunkHeaderSize;
        ArrayList< Object > curData=new ArrayList< Object >();
        for (Object o : data) {
            int padCnt=0;
            for (;;) {
                if (sizeCnt>=FragmentedObjectRepresentation.chunkWidth) {
                    assert !datas.isEmpty();
                    datas.add(curData);
                    sizeCnt=FragmentedObjectRepresentation.chunkHeaderSize;
                    curData=new ArrayList< Object >();
                }
                padCnt=0;
                while (((sizeCnt+padCnt)%sizeof(o))!=0) {
                    padCnt++;
                }
                if (sizeCnt+padCnt<FragmentedObjectRepresentation.chunkWidth) {
                    assert sizeCnt+padCnt+sizeof(o)<=FragmentedObjectRepresentation.chunkWidth;
                    break;
                }
                sizeCnt+=padCnt;
            }
            curData.add(new Padding(padCnt));
            sizeCnt+=padCnt;
            curData.add(o);
        }
        if (!curData.isEmpty()) {
            datas.add(curData);
        }
        
        return datas;
    }
    
    public static ArrayList< Object > buildFragmentedObjData(Pointerable self,
                                                             List< Object > data) {
        ArrayList< ArrayList< Object > > datas=
            buildFragmentedDatas(data);
        
        ArrayList< Object > chunkData=new ArrayList< Object >();
        
        int cnt=0;
        for (int i=0;i<datas.size();++i) {
            ArrayList< Object > cur=datas.get(i);
            cnt+=FragmentedObjectRepresentation.chunkWidth;
            
            if (i<datas.size()-1) {
                chunkData.add(
                    new TaggedLink(
                        new DisplacedLink(
                            self,cnt),
                        1));
            } else {
                chunkData.add(
                    new TaggedLink(
                        new Pointer(0),
                        1));
            }
            int size=Global.pointerSize;
            for (Object o : cur) {
                size+=sizeof(o);
                chunkData.add(o);
            }
            if (size<FragmentedObjectRepresentation.chunkWidth &&
                i<datas.size()-1) {
                chunkData.add(
                    new Padding(
                        FragmentedObjectRepresentation.chunkWidth-size));
            } else if (i==datas.size()-1) {
                int paddingCnt=0;
                while (((size+paddingCnt)%Global.pointerSize) != 0) {
                    paddingCnt++;
                }
                chunkData.add(
                    new Padding(paddingCnt));
            }
        }
        
        return chunkData;
    }
    
    public static LocalDataConstant buildFragmentedObj(String name,
                                                       List< Object > data) {
        return new LocalDataConstant(
            name,
            buildFragmentedObjData(new RemoteDataConstant(name),
                                   data));
    }
    
    public static String escapeXMLData(String str) {
        StringBuilder result=new StringBuilder();
        for (byte b : toUTF8(str)) {
            if (b=='<') {
                result.append("&lt;");
            } else if (b=='>') {
                result.append("&gt;");
            } else {
                result.append((char)b);
            }
        }
        return result.toString();
    }
    
    public static int stringSize(CharSequence source) {
        return 4+source.length()*2;
    }
    
    public static void writeString(ByteBuffer target,CharSequence source) {
        target.putInt(source.length());
        for (int i=0;i<source.length();++i) {
            target.putChar(source.charAt(i));
        }
    }
    
    public static String readString(ByteBuffer source) {
        int length=source.getInt();
        char[] chars=new char[length];
        int cnt=0;
        while (length-->0) {
            chars[cnt++]=source.getChar();
        }
        return new String(chars);
    }
    
    public static < T > boolean transitiveClosure(Set< T > set,
                                                  TwoWayMap< T, T > map,
                                                  Set< T > newSet) {
        int size=set.size();
        MyStack< T > worklist=new MyStack< T > ();
        for (T value : set) {
            worklist.push(value);
        }
        while (!worklist.empty()) {
            T cur = worklist.pop();
            Set< T > edges=map.valuesForKey(cur);
            for (T next : edges) {
                if (set.add(next)) {
                    worklist.push(next);
                    if (newSet!=null) {
                        newSet.add(next);
                    }
                }
            }
        }
        assert set.size()>=size;
        return set.size()>size;
    }
    
    public static void dumpMapJNI(
        String filename,
        TwoWayMap< ? extends JNINameable, ? extends JNINameable > map)
        throws IOException {

	PrintWriter fout=Util.wrap(new FileOutputStream(filename));
	try {
            for (JNINameable key : Util.sortJNI(map.keySet())) {
                fout.println(key.jniName());
                for (JNINameable val : Util.sortJNI(map.valuesForKey(key))) {
                    fout.println("   "+val.jniName());
                }
            }
	} finally {
	    fout.close();
	}
    }
    
    public static < T > T synchronizedSuck(Iterator< T > i) {
        synchronized (i) {
            if (i.hasNext()) {
                return i.next();
            } else {
                return null;
            }
        }
    }
    
    public static void startJoin(Iterable< ? extends Thread > threads) {
        try {
            if (Settings.PARALLEL_C1) {
                for (Thread t : threads) {
                    t.start();
                }
                for (Thread t : threads) {
                    t.join();
                }
            } else {
                for (Thread t : threads) {
                    t.run();
                }
            }
        } catch (Throwable e) {
            // checked exceptions.  they're good for you.  trust me.
            throw new Error(e);
        }
    }
    
    public static void startJoin(int jobs,
                                 final HappyRunnable runnable) {
        if (!Settings.PARALLEL_C1) {
            jobs=1;
        }
        ArrayList< HappyThread > threads=new ArrayList< HappyThread >();
        while (jobs-->0) {
            threads.add(new HappyThread(runnable.toString()+"-"+(jobs+1)) {
                    public void doStuff() throws Throwable {
                        runnable.doStuff();
                    }
                });
        }
        startJoin(threads);
    }

    @SuppressWarnings("unchecked")
    public static < T > T[] append(T[] one,T[] two) {
        T[] result=(T[])Array.newInstance(one.getClass().getComponentType(),
                                          one.length+two.length);
        System.arraycopy(one,0,
                         result,0,
                         one.length);
        System.arraycopy(two,0,
                         result,one.length,
                         two.length);
        return result;
    }
    
    public static Arg generateIRForTableLoad(Code code,
                                             Operation before,
                                             int index,
                                             VisibleClass contents,
                                             Arg objTablePtr) {
        Var objPtr=code.addVar(Exectype.POINTER,
                               contents.asExectype());
        before.prepend(
            new SimpleInst(
                before.di(),OpCode.Add,
                objPtr,new Arg[]{
                    objTablePtr,
                    PointerConst.make(
                        Global.allocOffset+
                        contents.alignedPayloadSize()*index)
                }));
        return objPtr;
    }
    
    public static Arg generateIRForTableLoad(Code code,
                                             Operation before,
                                             int index,
                                             VisibleClass contents,
                                             Linkable table) {
        Var objTablePtr=code.addVar(Exectype.POINTER,
                                    contents.asExectype());
        before.prepend(
            new CFieldInst(
                before.di(),OpCode.GetCVarAddress,
                objTablePtr,Arg.EMPTY,
                table));
        return generateIRForTableLoad(
            code,before,index,contents,objTablePtr);
    }
    
    public static Arg generateIRForTableLoad(Code code,
                                             Operation before,
                                             int index,
                                             VisibleClass contents,
                                             CStructField payloadField) {
        Var vmPtr=code.addVar(Exectype.POINTER);
        Var payloadPtr=code.addVar(Exectype.POINTER);
        Var objTablePtr=code.addVar(Exectype.POINTER);
        before.prepend(
            new CFieldInst(
                before.di(),OpCode.GetCField,
                vmPtr,new Arg[]{
                    Arg.THREAD_STATE
                },
                CTypesystemReferences.ThreadState_vm));
        before.prepend(
            new CFieldInst(
                before.di(),OpCode.GetCField,
                payloadPtr,new Arg[]{
                    vmPtr
                },
                CTypesystemReferences.VM_payload));
        before.prepend(
            new CFieldInst(
                before.di(),OpCode.GetCField,
                objTablePtr,new Arg[]{
                    payloadPtr
                },
                payloadField));
        return generateIRForTableLoad(
            code,before,index,contents,objTablePtr);
    }
    
    public static Arg generateIRForTableLoad(Code code,
                                             Operation before,
                                             int index,
                                             VisibleClass contents,
                                             CStructField payloadField,
                                             Linkable table) {
        if (Global.oneShotPayload) {
            return generateIRForTableLoad(code,before,index,contents,table);
        } else {
            return generateIRForTableLoad(code,before,index,contents,payloadField);
        }
    }
    
    public static String asmConstant(Basetype type,Object value) {
        if (value instanceof Pointer) {
            return asmConstant(type,((Pointer)value).value());
        }
        switch (type) {
        case BYTE:
        case BOOLEAN:
            return "\t.byte "+((Number)value).byteValue()+"\n";
        case SHORT:
        case CHAR:
            return "\t.value "+((Number)value).shortValue()+"\n";
        case INT:
            return "\t.long "+((Number)value).intValue()+"\n";
        case LONG:
            if (Global.pointerSize==4) {
                long v=((Number)value).longValue();
                if (Settings.IS_LITTLE_ENDIAN) {
                    return 
                        "\t.long "+((v>>0)&0xffffffffl)+"\n"+
                        "\t.long "+((v>>32)&0xffffffffl)+"\n";
                } else {
                    return 
                        "\t.long "+((v>>32)&0xffffffffl)+"\n"+
                        "\t.long "+((v>>0)&0xffffffffl)+"\n";
                }
            } else {
                return "\t.quad "+((Number)value).longValue()+"\n";
            }
        case POINTER:
            if (Global.pointerSize==4) {
                return asmConstant(Basetype.INT,value);
            } else {
                return asmConstant(Basetype.LONG,value);
            }
        case FLOAT:
            return asmConstant(Basetype.INT,
                               Float.floatToRawIntBits(((Number)value).floatValue()));
        case DOUBLE:
            return asmConstant(Basetype.LONG,
                               Double.doubleToRawLongBits(((Number)value).doubleValue()));
        default:
            throw new CompilerException("Cannot generate asm constant for type "+type);
        }
    }
    
    public static long fiatToLong(Basetype type,Object value) {
        if (value instanceof Pointer) {
            return fiatToLong(type,((Pointer)value).value());
        }
        switch (type) {
        case BYTE:
        case BOOLEAN:
            return ((Number)value).byteValue();
        case SHORT:
        case CHAR:
            return ((Number)value).shortValue();
        case INT:
            return ((Number)value).intValue();
        case LONG:
            return ((Number)value).longValue();
        case POINTER:
            if (Global.pointerSize==4) {
                return fiatToLong(Basetype.INT,value);
            } else {
                return fiatToLong(Basetype.LONG,value);
            }
        case FLOAT:
            return fiatToLong(Basetype.INT,
                              Float.floatToRawIntBits(((Number)value).floatValue()));
        case DOUBLE:
            return fiatToLong(Basetype.LONG,
                              Double.doubleToRawLongBits(((Number)value).doubleValue()));
        default:
            throw new CompilerException("Cannot fiat to long type "+type);
        }
    }
    
    public static StaticCGlobal makeFloatConst(Basetype type,
                                               Object value) {
        String hex;
        if (value instanceof Float) {
            hex=Integer.toHexString(Float.floatToRawIntBits((Float)value));
        } else {
            assert value instanceof Double;
            hex=Long.toHexString(Double.doubleToRawLongBits((Double)value));
        }
        
        return new StaticCGlobal(type,
                                 "Const"+type+"_"+Util.hidePunct(""+value)+"_"+hex,
                                 value);
    }
}

