/*
 * fivmr_jni.h
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

#ifndef FP_FIVMR_JNI_H
#define FP_FIVMR_JNI_H

#include <inttypes.h>
#include <stdarg.h>

#define JNIEXPORT
#define JNIIMPORT
#define JNICALL

typedef int8_t jboolean;
typedef int8_t jbyte;
typedef uint16_t jchar;
typedef int16_t jshort;
typedef int32_t jint;
typedef int64_t jlong;
typedef float jfloat;
typedef double jdouble;

#define JNI_FALSE 0
#define JNI_TRUE 1

#define JNI_COMMIT 1
#define JNI_ABORT 2

#define JNI_OK           0                 /* success */
#define JNI_ERR          (-1)              /* unknown error */
#define JNI_EDETACHED    (-2)              /* thread detached from the VM */
#define JNI_EVERSION     (-3)              /* JNI version error */
#define JNI_ENOMEM       (-4)              /* not enough memory */
#define JNI_EEXIST       (-5)              /* VM already created */
#define JNI_EINVAL       (-6)              /* invalid arguments */

typedef jint jsize;

struct _jobject {};

typedef struct _jobject *jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef jobject jarray;
typedef jobject jobjectArray;
typedef jobject jbooleanArray;
typedef jobject jbyteArray;
typedef jobject jcharArray;
typedef jobject jshortArray;
typedef jobject jintArray;
typedef jobject jlongArray;
typedef jobject jfloatArray;
typedef jobject jdoubleArray;
typedef jobject jthrowable;

typedef jobject jweak;

struct _jfieldID {};
typedef struct _jfieldID *jfieldID;
struct _jmethodID {};
typedef struct _jmethodID *jmethodID;

typedef union jvalue {
    jboolean z;
    jbyte b;
    jchar c;
    jshort s;
    jint i;
    jlong j;
    jfloat f;
    jdouble d;
    jobject l;
} jvalue;

typedef struct {
    char *name;
    char *signature;
    void *fnPtr;
} JNINativeMethod;

struct JNINativeInterface;

typedef const struct JNINativeInterface *JNIEnv;

struct JNIInvokeInterface;

typedef const struct JNIInvokeInterface *JavaVM;

struct JNIInvokeInterface {
    void *_reserved1;
    void *_reserved2;
    void *_reserved3;
    jint (*DestroyJavaVM)(JavaVM *vm);
    jint (*AttachCurrentThread)(JavaVM *vm,JNIEnv **penv,void *args);
    jint (*DetachCurrentThread)(JavaVM *vm);
    jint (*GetEnv)(JavaVM *vm,JNIEnv **penv,jint version);
    jint (*AttachCurrentThreadAsDaemon)(JavaVM *vm,JNIEnv **penv,void *args);
};

struct JNINativeInterface {
    void *_reserved1;
    void *_reserved2;
    void *_reserved3;
    void *_reserved4;

    jint (*GetVersion)(JNIEnv *env);
    jclass (*DefineClass)(JNIEnv *env, jobject loader, const jbyte *buf, jsize bufLen);
    jclass (*FindClass)(JNIEnv *env, const char *name);

    void *_reserved5;
    void *_reserved6;
    void *_reserved7;

    jclass (*GetSuperclass)(JNIEnv *env, jclass clazz);
    jboolean (*IsAssignableFrom)(JNIEnv *env, jclass clazz1, jclass clazz2);

    void *_reserved8;

    jint (*Throw)(JNIEnv *env, jthrowable obj);
    jint (*ThrowNew)(JNIEnv *env, jclass clazz, const char *message);

    jthrowable (*ExceptionOccurred)(JNIEnv *env);
    void (*ExceptionDescribe)(JNIEnv *env);
    void (*ExceptionClear)(JNIEnv *env);

    void (*FatalError)(JNIEnv *env, const char *msg);

    void *_reserved9;
    void *_reserved10;

    jobject (*NewGlobalRef)(JNIEnv *env, jobject obj);
    void (*DeleteGlobalRef)(JNIEnv *env, jobject globalRef);
    void (*DeleteLocalRef)(JNIEnv *env, jobject localRef);

    jboolean (*IsSameObject)(JNIEnv *env, jobject ref1, jobject ref2);

    void *_reserved11;
    void *_reserved12;

    jobject (*AllocObject)(JNIEnv *env, jclass clazz);

    jobject (*NewObject)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jobject (*NewObjectA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jobject (*NewObjectV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);

    jclass (*GetObjectClass)(JNIEnv *env, jobject obj);
    jboolean (*IsInstanceOf)(JNIEnv *env, jobject obj, jclass clazz);

    jmethodID (*GetMethodID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (*CallObjectMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jobject (*CallObjectMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jobject (*CallObjectMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jboolean (*CallBooleanMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jboolean (*CallBooleanMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jboolean (*CallBooleanMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jbyte (*CallByteMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jbyte (*CallByteMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jbyte (*CallByteMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jchar (*CallCharMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jchar (*CallCharMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jchar (*CallCharMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jshort (*CallShortMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jshort (*CallShortMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jshort (*CallShortMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jint (*CallIntMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jint (*CallIntMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jint (*CallIntMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jlong (*CallLongMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jlong (*CallLongMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jlong (*CallLongMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jfloat (*CallFloatMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jfloat (*CallFloatMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jfloat (*CallFloatMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jdouble (*CallDoubleMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jdouble (*CallDoubleMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    jdouble (*CallDoubleMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    void (*CallVoidMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);
    void (*CallVoidMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
    void (*CallVoidMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);

    jobject (*CallNonvirtualObjectMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jobject (*CallNonvirtualObjectMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jobject (*CallNonvirtualObjectMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jboolean (*CallNonvirtualBooleanMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jboolean (*CallNonvirtualBooleanMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jboolean (*CallNonvirtualBooleanMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jbyte (*CallNonvirtualByteMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jbyte (*CallNonvirtualByteMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jbyte (*CallNonvirtualByteMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jchar (*CallNonvirtualCharMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jchar (*CallNonvirtualCharMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jchar (*CallNonvirtualCharMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jshort (*CallNonvirtualShortMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jshort (*CallNonvirtualShortMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jshort (*CallNonvirtualShortMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jint (*CallNonvirtualIntMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jint (*CallNonvirtualIntMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jint (*CallNonvirtualIntMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jlong (*CallNonvirtualLongMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jlong (*CallNonvirtualLongMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jlong (*CallNonvirtualLongMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jfloat (*CallNonvirtualFloatMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jfloat (*CallNonvirtualFloatMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jfloat (*CallNonvirtualFloatMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    jdouble (*CallNonvirtualDoubleMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jdouble (*CallNonvirtualDoubleMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    jdouble (*CallNonvirtualDoubleMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);
    void (*CallNonvirtualVoidMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    void (*CallNonvirtualVoidMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);
    void (*CallNonvirtualVoidMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);

    jfieldID (*GetFieldID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (*GetObjectField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jboolean (*GetBooleanField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jbyte (*GetByteField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jchar (*GetCharField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jshort (*GetShortField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jint (*GetIntField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jlong (*GetLongField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jfloat (*GetFloatField)(JNIEnv *env, jobject obj, jfieldID fieldID);
    jdouble (*GetDoubleField)(JNIEnv *env, jobject obj, jfieldID fieldID);

    void (*SetObjectField)(JNIEnv *env, jobject obj, jfieldID fieldID, jobject value);
    void (*SetBooleanField)(JNIEnv *env, jobject obj, jfieldID fieldID, jboolean value);
    void (*SetByteField)(JNIEnv *env, jobject obj, jfieldID fieldID, jbyte value);
    void (*SetCharField)(JNIEnv *env, jobject obj, jfieldID fieldID, jchar value);
    void (*SetShortField)(JNIEnv *env, jobject obj, jfieldID fieldID, jshort value);
    void (*SetIntField)(JNIEnv *env, jobject obj, jfieldID fieldID, jint value);
    void (*SetLongField)(JNIEnv *env, jobject obj, jfieldID fieldID, jlong value);
    void (*SetFloatField)(JNIEnv *env, jobject obj, jfieldID fieldID, jfloat value);
    void (*SetDoubleField)(JNIEnv *env, jobject obj, jfieldID fieldID, jdouble value);

    jmethodID (*GetStaticMethodID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (*CallStaticObjectMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jobject (*CallStaticObjectMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jobject (*CallStaticObjectMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jboolean (*CallStaticBooleanMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jboolean (*CallStaticBooleanMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jboolean (*CallStaticBooleanMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jbyte (*CallStaticByteMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jbyte (*CallStaticByteMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jbyte (*CallStaticByteMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jchar (*CallStaticCharMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jchar (*CallStaticCharMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jchar (*CallStaticCharMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jshort (*CallStaticShortMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jshort (*CallStaticShortMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jshort (*CallStaticShortMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jint (*CallStaticIntMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jint (*CallStaticIntMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jint (*CallStaticIntMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jlong (*CallStaticLongMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jlong (*CallStaticLongMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jlong (*CallStaticLongMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jfloat (*CallStaticFloatMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jfloat (*CallStaticFloatMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jfloat (*CallStaticFloatMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jdouble (*CallStaticDoubleMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jdouble (*CallStaticDoubleMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    jdouble (*CallStaticDoubleMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    void (*CallStaticVoidMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    void (*CallStaticVoidMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
    void (*CallStaticVoidMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);

    jfieldID (*GetStaticFieldID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (*GetStaticObjectField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jboolean (*GetStaticBooleanField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jbyte (*GetStaticByteField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jchar (*GetStaticCharField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jshort (*GetStaticShortField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jint (*GetStaticIntField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jlong (*GetStaticLongField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jfloat (*GetStaticFloatField)(JNIEnv *env, jclass clazz, jfieldID fieldID);
    jdouble (*GetStaticDoubleField)(JNIEnv *env, jclass clazz, jfieldID fieldID);

    void (*SetStaticObjectField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value);
    void (*SetStaticBooleanField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jboolean value);
    void (*SetStaticByteField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jbyte value);
    void (*SetStaticCharField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jchar value);
    void (*SetStaticShortField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jshort value);
    void (*SetStaticIntField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jint value);
    void (*SetStaticLongField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jlong value);
    void (*SetStaticFloatField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jfloat value);
    void (*SetStaticDoubleField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jdouble value);

    jstring (*NewString)(JNIEnv *env, const jchar *unicodeChars, jsize len);
    jsize (*GetStringLength)(JNIEnv *env, jstring string);
    const jchar *(*GetStringChars)(JNIEnv *env,jstring string, jboolean *isCopy);
    void (*ReleaseStringChars)(JNIEnv *env, jstring string, const jchar *chars);

    jstring (*NewStringUTF)(JNIEnv *env, const char *bytes);
    jsize (*GetStringUTFLength)(JNIEnv *env, jstring string);
    const char *(*GetStringUTFChars)(JNIEnv *env, jstring string, jboolean *isCopy);
    void (*ReleaseStringUTFChars)(JNIEnv *env, jstring string, const char *utf);

    jsize (*GetArrayLength)(JNIEnv *env, jarray array);

    jobjectArray (*NewObjectArray)(JNIEnv *env, jsize length, jclass elementClass, jobject initialElement);
    jobject (*GetObjectArrayElement)(JNIEnv *env, jobjectArray array, jsize index);
    void (*SetObjectArrayElement)(JNIEnv *env, jobjectArray array, jsize index, jobject value);
    
    jbooleanArray (*NewBooleanArray)(JNIEnv *env, jsize length);
    jbyteArray (*NewByteArray)(JNIEnv *env, jsize length);
    jcharArray (*NewCharArray)(JNIEnv *env, jsize length);
    jshortArray (*NewShortArray)(JNIEnv *env, jsize length);
    jintArray (*NewIntArray)(JNIEnv *env, jsize length);
    jlongArray (*NewLongArray)(JNIEnv *env, jsize length);
    jfloatArray (*NewFloatArray)(JNIEnv *env, jsize length);
    jdoubleArray (*NewDoubleArray)(JNIEnv *env, jsize length);
    
    jboolean *(*GetBooleanArrayElements)(JNIEnv *env, jbooleanArray array, jboolean *isCopy);
    jbyte *(*GetByteArrayElements)(JNIEnv *env, jbyteArray array, jboolean *isCopy);
    jchar *(*GetCharArrayElements)(JNIEnv *env, jcharArray array, jboolean *isCopy);
    jshort *(*GetShortArrayElements)(JNIEnv *env, jshortArray array, jboolean *isCopy);
    jint *(*GetIntArrayElements)(JNIEnv *env, jintArray array, jboolean *isCopy);
    jlong *(*GetLongArrayElements)(JNIEnv *env, jlongArray array, jboolean *isCopy);
    jfloat *(*GetFloatArrayElements)(JNIEnv *env, jfloatArray array, jboolean *isCopy);
    jdouble *(*GetDoubleArrayElements)(JNIEnv *env, jdoubleArray array, jboolean *isCopy);
    
    void (*ReleaseBooleanArrayElements)(JNIEnv *env, jbooleanArray array, jboolean *elems, jint mode);
    void (*ReleaseByteArrayElements)(JNIEnv *env, jbyteArray array, jbyte *elems, jint mode);
    void (*ReleaseCharArrayElements)(JNIEnv *env, jcharArray array, jchar *elems, jint mode);
    void (*ReleaseShortArrayElements)(JNIEnv *env, jshortArray array, jshort *elems, jint mode);
    void (*ReleaseIntArrayElements)(JNIEnv *env, jintArray array, jint *elems, jint mode);
    void (*ReleaseLongArrayElements)(JNIEnv *env, jlongArray array, jlong *elems, jint mode);
    void (*ReleaseFloatArrayElements)(JNIEnv *env, jfloatArray array, jfloat *elems, jint mode);
    void (*ReleaseDoubleArrayElements)(JNIEnv *env, jdoubleArray array, jdouble *elems, jint mode);
    
    void (*GetBooleanArrayRegion)(JNIEnv *env, jbooleanArray array, jsize start, jsize len, jboolean *buf);
    void (*GetByteArrayRegion)(JNIEnv *env, jbyteArray array, jsize start, jsize len, jbyte *buf);
    void (*GetCharArrayRegion)(JNIEnv *env, jcharArray array, jsize start, jsize len, jchar *buf);
    void (*GetShortArrayRegion)(JNIEnv *env, jshortArray array, jsize start, jsize len, jshort *buf);
    void (*GetIntArrayRegion)(JNIEnv *env, jintArray array, jsize start, jsize len, jint *buf);
    void (*GetLongArrayRegion)(JNIEnv *env, jlongArray array, jsize start, jsize len, jlong *buf);
    void (*GetFloatArrayRegion)(JNIEnv *env, jfloatArray array, jsize start, jsize len, jfloat *buf);
    void (*GetDoubleArrayRegion)(JNIEnv *env, jdoubleArray array, jsize start, jsize len, jdouble *buf);

    void (*SetBooleanArrayRegion)(JNIEnv *env, jbooleanArray array, jsize start, jsize len, jboolean *buf);
    void (*SetByteArrayRegion)(JNIEnv *env, jbyteArray array, jsize start, jsize len, jbyte *buf);
    void (*SetCharArrayRegion)(JNIEnv *env, jcharArray array, jsize start, jsize len, jchar *buf);
    void (*SetShortArrayRegion)(JNIEnv *env, jshortArray array, jsize start, jsize len, jshort *buf);
    void (*SetIntArrayRegion)(JNIEnv *env, jintArray array, jsize start, jsize len, jint *buf);
    void (*SetLongArrayRegion)(JNIEnv *env, jlongArray array, jsize start, jsize len, jlong *buf);
    void (*SetFloatArrayRegion)(JNIEnv *env, jfloatArray array, jsize start, jsize len, jfloat *buf);
    void (*SetDoubleArrayRegion)(JNIEnv *env, jdoubleArray array, jsize start, jsize len, jdouble *buf);
    
    jint (*RegisterNatives)(JNIEnv *env, jclass clazz, const JNINativeMethod *methods, jint nmethods);
    jint (*UnregisterNatives)(JNIEnv *env, jclass clazz);
    
    jint (*MonitorEnter)(JNIEnv *env, jobject obj);
    jint (*MonitorExit)(JNIEnv *env, jobject obj);
    
    jint (*GetJavaVM)(JNIEnv *env, JavaVM **vm);

    void (*GetStringRegion)(JNIEnv *env,jstring str,jsize start,jsize len,jchar *buf);
    void (*GetStringUTFRegion)(JNIEnv *env,jstring str,jsize start,jsize len,char *buf);;
    
    void *(*GetPrimitiveArrayCritical)(JNIEnv *env,jarray array,jboolean *isCopy);
    void (*ReleasePrimitiveArrayCritical)(JNIEnv *env,jarray array,void *carray,jint mode);
    
    const jchar *(*GetStringCritical)(JNIEnv *env,jstring string,jboolean *isCopy);
    void (*ReleaseStringCritical)(JNIEnv *env,jstring string,const jchar *carray);
    
    jweak (*NewWeakGlobalRef)(JNIEnv *env,jobject obj);
    void (*DeleteWeakGlobalRef)(JNIEnv *env,jweak obj);
    
    jboolean (*ExceptionCheck)(JNIEnv *env);
    
    jobject (*NewDirectByteBuffer)(JNIEnv *env,void *address,jlong capacity);
    void *(*GetDirectBufferAddress)(JNIEnv *env,jobject buf);
    jlong (*GetDirectBufferCapacity)(JNIEnv *env,jobject buf);
};

jint JNI_GetDefaultJavaVMInitArgs(void *args);
jint JNI_CreateJavaVM(JavaVM **pvm,void **penv,void *args);
jint JNI_GetCreatedJavaVMs(JavaVM **,jsize,jsize*); /* wtf? */

#define JNI_VERSION_1_1 0x00010001
#define JNI_VERSION_1_2 0x00010002
#define JNI_VERSION_1_4 0x00010004

#endif

