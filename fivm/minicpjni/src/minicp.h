#ifndef MINICP_H
#define MINICP_H

#include <fivmr_jni.h>
#include <stdlib.h>
#include "cpio.h"
jclass JCL_FindClass (JNIEnv * env, const char *className);
void JCL_ThrowException (JNIEnv * env, const char *className, const char *errMsg);
void *JCL_GetRawData (JNIEnv * env, jobject rawdata);
void *JCL_malloc (JNIEnv * env, size_t size);
void *JCL_realloc (JNIEnv * env, void *ptr, size_t size);
void JCL_free (JNIEnv * env, void *p);
const char* JCL_jstring_to_cstring (JNIEnv * env, jstring s);
void JCL_free_cstring (JNIEnv * env, jstring s, const char *cstr);
#endif
