/*
 * fijitest.c -- JNI tests
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

#include <jni.h>
#include <stdio.h>

static void printStuff(JNIEnv *env,
                       const char *fromWhere,
                       jclass cls) {
    jclass classCls;
    jmethodID getName;
    jstring clsName;
    const char *clsNameStr;
    jboolean isCopy;
    
    classCls=(*env)->FindClass(env,"java/lang/Class");
    getName=(*env)->GetMethodID(env,classCls,"getName","()Ljava/lang/String;");
    
    if (classCls==0 || getName==0) {
        fprintf(stderr,
                "ERROR: could not find getName() method on java.lang.Class\n");
        return;
    }
    
    clsName=(*env)->CallObjectMethod(env,cls,getName);
    
    clsNameStr=(*env)->GetStringUTFChars(env,clsName,&isCopy);
    
    printf("%s called from %s\n",fromWhere,clsNameStr);
    
    if (isCopy) {
        (*env)->ReleaseStringUTFChars(env,clsName,clsNameStr);
    }
}

static void printStuff2(JNIEnv *env,
                        const char *fromWhere,
                        jobject obj) {
    jclass objectCls;
    jmethodID toString;
    jstring str;
    const char *strStr;
    jboolean isCopy;
    
    objectCls=(*env)->FindClass(env,"java/lang/Object");
    toString=(*env)->GetMethodID(env,objectCls,"toString","()Ljava/lang/String;");
    
    if (objectCls==0 || toString==0) {
        fprintf(stderr,
                "ERROR: could not find toString() method on java.lang.Object\n");
        return;
    }
    
    str=(*env)->CallObjectMethod(env,obj,toString);
    
    strStr=(*env)->GetStringUTFChars(env,str,&isCopy);
    
    printf("%s called from %s\n",fromWhere,strStr);
    
    if (isCopy) {
        (*env)->ReleaseStringUTFChars(env,str,strStr);
    }
}

JNIEXPORT void JNICALL
Java_com_fiji_fivm_test_JniTest_simpleTest(JNIEnv *env, jclass cls) {
    printStuff(env,"simpleTest()",cls);
}

JNIEXPORT void JNICALL
Java_com_fiji_fivm_test_JniTest_simpleSyncTest(JNIEnv *env, jclass cls) {
    printStuff(env,"simpleSyncTest()",cls);
}

JNIEXPORT jobject JNICALL
Java_com_fiji_fivm_test_JniTest_simpleRetTest(JNIEnv *env, jclass cls, jobject obj) {
    printStuff(env,"simpleRetTest()",cls);
    return obj;
}

JNIEXPORT jobject JNICALL
Java_com_fiji_fivm_test_JniTest_simpleSyncRetTest(JNIEnv *env, jclass cls, jobject obj) {
    printStuff(env,"simpleSyncRetTest()",cls);
    return obj;
}

JNIEXPORT void JNICALL
Java_com_fiji_fivm_test_JniTest_simpleThrowTest(JNIEnv *env, jclass cls) {
    printStuff(env,"simpleThrowTest()",cls);
    (*env)->ThrowNew(env,
                     (*env)->FindClass(env,
                                       "com/fiji/fivm/test/JniTest$MyException"),
                     "Hi, I'm an exception, thrown from native code!");
}

JNIEXPORT void JNICALL
Java_com_fiji_fivm_test_JniTest_simpleSyncThrowTest(JNIEnv *env, jclass cls) {
    printStuff(env,"simpleSyncThrowTest()",cls);
    (*env)->ThrowNew(env,
                     (*env)->FindClass(env,
                                       "com/fiji/fivm/test/JniTest$MyException"),
                     "Hi, I'm an exception, thrown from native synchronized code!");
}

JNIEXPORT void JNICALL
Java_com_fiji_fivm_test_JniTest2_twoInts(JNIEnv *env, jclass cls,
                                         jint a, jint b) {
    printStuff(env,"twoInts()",cls);
    printf("a = %d, b = %d\n",a,b);
}

JNIEXPORT jint JNICALL
Java_com_fiji_fivm_test_JniTest3_twoInts(JNIEnv *env, jobject obj,
                                         jint a, jint b) {
    jclass myCls;
    jfieldID meFld;
    jint me;
    
    myCls=(*env)->GetObjectClass(env,obj);
    meFld=(*env)->GetFieldID(env,myCls,"me","I");
    
    if (myCls==0 || meFld==0) {
        fprintf(stderr,"ERROR: could not find field me in JniTest3\n");
        return -1;
    }
    
    me=(*env)->GetIntField(env,obj,meFld);
    
    printStuff2(env,"twoInts()",obj);
    
    printf("a = %d, b = %d, me = %d\n",a,b,me);
    
    return a+b+me;
}

