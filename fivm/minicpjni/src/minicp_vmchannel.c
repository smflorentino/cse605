/* Copyright (C) 1998, 2005, 2006, 2008 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

#include "minicp.h"
#include <rtems.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include "cpio.h"
#include "javanio.h"

#define CONNECT_EXCEPTION "java/net/ConnectException"
#define IO_EXCEPTION "java/io/IOException"
#define SOCKET_EXCEPTION "java/net/SocketException"
#define INTERRUPTED_IO_EXCEPTION "java/io/InterruptedIOException"
#define NON_READABLE_CHANNEL_EXCEPTION "java/nio/channels/NonReadableChannelException"
#define NON_WRITABLE_CHANNEL_EXCEPTION "java/nio/channels/NonWritableChannelException"
#define SOCKET_TIMEOUT_EXCEPTION "java/net/SocketTimeoutException"

/* Align a value up or down to a multiple of the pagesize. */
#define ALIGN_DOWN(p,s) ((p) - ((p) % (s)))
#define ALIGN_UP(p,s) ((p) + ((s) - ((p) % (s))))

enum JCL_buffer_type { DIRECT, HEAP, ARRAY, UNKNOWN };

struct JCL_buffer
{
  enum JCL_buffer_type type;
  jbyte *ptr;
  jint offset;
  jint position;
  jint limit;
  jint count;
};

static jfieldID address_fid;
static jmethodID get_position_mid;
static jmethodID set_position_mid;
static jmethodID get_limit_mid;
static jmethodID set_limit_mid;
static jmethodID has_array_mid;
static jmethodID array_mid;
static jmethodID array_offset_mid;
static jmethodID thread_interrupted_mid;
static jclass vm_channel_class;

static jboolean
is_non_blocking_fd(jint fd)
{
  int opts;
  opts = fcntl(fd, F_GETFL);
  if (opts == -1)
    {
      /* Assume blocking on error. */
      return 0;
    }
  return (opts & O_NONBLOCK) != 0;
}


static jmethodID
get_method_id(JNIEnv *env,  jclass clazz, const char *name, 
	          const char *sig)
{
  jmethodID mid = (*env)->GetMethodID(env, clazz, name, sig);
/*   NIODBG("name: %s; sig: %s", name, sig); */
  if (mid == NULL)
    {
      JCL_ThrowException(env, "java/lang/InternalError", name);
      return NULL;
    }
  
  return mid;
}

int
JCL_init_buffer(JNIEnv *env, struct JCL_buffer *buf, jobject bbuf)
{
  void *addr = (*env)->GetDirectBufferAddress (env, bbuf);

/*   NIODBG("buf: %p; bbuf: %p; addr: %p", (void *) buf, bbuf, addr); */
  
  buf->position = (*env)->CallIntMethod(env, bbuf, get_position_mid);
  buf->limit = (*env)->CallIntMethod(env, bbuf, get_limit_mid);
  buf->offset = 0;
  buf->count = 0;
  buf->type = UNKNOWN;
    
  if (addr != NULL)
    {
      buf->ptr = (jbyte *) addr;
      buf->type = DIRECT;
    }
  else
    {
      jboolean has_array;
      has_array = (*env)->CallBooleanMethod(env, bbuf, has_array_mid);
      
      if (has_array == JNI_TRUE)
        {
          jbyteArray arr;
          buf->offset = (*env)->CallIntMethod(env, bbuf, array_offset_mid);
          arr = (*env)->CallObjectMethod(env, bbuf, array_mid);
          buf->ptr = (*env)->GetByteArrayElements(env, arr, 0);
          buf->type = ARRAY;
          (*env)->DeleteLocalRef(env, arr);
        }
      else
        {
          jobject address = (*env)->GetObjectField (env, bbuf, address_fid);
          if (address == NULL)
            return -1; /* XXX handle non-array, non-native buffers? */
          buf->ptr = (jbyte *) JCL_GetRawData(env, address);
          buf->type = HEAP;
          (*env)->DeleteLocalRef(env, address);
        }
    }
      
  return 0;
}

void
JCL_release_buffer(JNIEnv *env, struct JCL_buffer *buf, jobject bbuf, 
    jint action)
{
  jbyteArray arr;

/*   NIODBG("buf: %p; bbuf: %p; action: %x", (void *) buf, bbuf, action); */
  
  /* Set the position to the appropriate value */
  if (buf->count > 0)
    {
      jobject bbufTemp;
      bbufTemp = (*env)->CallObjectMethod(env, bbuf, set_position_mid, 
                                          buf->position + buf->count);
      (*env)->DeleteLocalRef(env, bbufTemp);
    }
    
  switch (buf->type)
    {
    case DIRECT:
    case HEAP:
      break;
    case ARRAY:
      arr = (*env)->CallObjectMethod(env, bbuf, array_mid);
      (*env)->ReleaseByteArrayElements(env, arr, buf->ptr, action);
      (*env)->DeleteLocalRef(env, arr);
      break;
    case UNKNOWN:
      /* TODO: Handle buffers that are not direct or array backed */
      break;
    }
}

int
JCL_thread_interrupted(JNIEnv *env)
{
  return (int) (*env)->CallStaticBooleanMethod(env, vm_channel_class,
					       thread_interrupted_mid);
}

JNIEXPORT void JNICALL 
Java_gnu_java_nio_VMChannel_initIDs  (JNIEnv *env, 
	jclass clazz)
{
  jclass bufferClass = JCL_FindClass(env, "java/nio/Buffer");
  jclass byteBufferClass = JCL_FindClass(env, "java/nio/ByteBuffer");

/*   NIODBG("%s", "..."); */

  address_fid = (*env)->GetFieldID(env, bufferClass, "address", 
                                   "Lgnu/classpath/Pointer;");
  if (address_fid == NULL)
    {
  	  JCL_ThrowException(env, "java/lang/InternalError", 
  	  	"Unable to find internal field");
      return;
    }
  
  get_position_mid = get_method_id(env, bufferClass, "position", "()I");
  set_position_mid = get_method_id(env, bufferClass, "position", 
                                   "(I)Ljava/nio/Buffer;");
  get_limit_mid = get_method_id(env, bufferClass, "limit", "()I");
  set_limit_mid = get_method_id(env, bufferClass, "limit", 
                                "(I)Ljava/nio/Buffer;");
  has_array_mid = get_method_id(env, byteBufferClass, "hasArray", "()Z");
  array_mid = get_method_id(env, byteBufferClass, "array", "()[B");
  array_offset_mid = get_method_id(env, byteBufferClass, "arrayOffset", "()I");
  
  vm_channel_class = clazz;
  thread_interrupted_mid = (*env)->GetStaticMethodID(env, clazz,
                                                  "isThreadInterrupted",
                                                  "()Z");
}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    stdin_fd
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_gnu_java_nio_VMChannel_stdin_1fd (JNIEnv *env __attribute__((unused)),
                                       jclass c __attribute__((unused)))
{
/*   NIODBG("%d", fileno (stdin)); */
  return fileno (stdin);
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    stdout_fd
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_gnu_java_nio_VMChannel_stdout_1fd (JNIEnv *env __attribute__((unused)),
                                       jclass c __attribute__((unused)))
{
/*   NIODBG("%d", fileno (stdout)); */
  return fileno (stdout);
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    stderr_fd
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_gnu_java_nio_VMChannel_stderr_1fd (JNIEnv *env __attribute__((unused)),
                                       jclass c __attribute__((unused)))
{
/*   NIODBG("%d", fileno (stderr)); */
  return fileno (stderr);
}

JNIEXPORT jint JNICALL 
Java_gnu_java_nio_VMChannel_read__ILjava_nio_ByteBuffer_2 (JNIEnv *env,
                                                           jobject o __attribute__ ((__unused__)), 
                                                           jint fd, 
                                                           jobject bbuf)
{

  jint len;
  ssize_t result;
  struct JCL_buffer buf;
  int tmp_errno;
  
/*   NIODBG("fd: %d; bbuf: %p", fd, bbuf); */
  
  if (JCL_init_buffer(env, &buf, bbuf) < 0)
    {
      /* TODO: Rethrown exception */
      JCL_ThrowException (env, IO_EXCEPTION, "Buffer initialisation failed");
      return -1;
    }

  len = buf.limit - buf.position;

  if (len == 0)
    {
		
      JCL_release_buffer (env, &buf, bbuf, JNI_ABORT);
      return 0;
    }
  
  do 
    {
      result = cpnio_read (fd, &(buf.ptr[buf.position + buf.offset]), len);
      tmp_errno = errno;
    }
  while (result == -1 && errno == EINTR);// && ! JCL_thread_interrupted(env));
  errno = tmp_errno;
  
  if (result == 0)
    {
      result = -1;
      buf.count = 0;
    }
  else if (result == -1)
    {
      buf.count = 0;
      if (errno == EAGAIN)
        {
          if (is_non_blocking_fd(fd))
            {
              /* Non-blocking */
              result = 0;
            }
          else
            {
              /* Read timeout on a socket with SO_RCVTIMEO != 0. */
              JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
              JCL_ThrowException(env, SOCKET_TIMEOUT_EXCEPTION, "read timed out");
              return -1;
            }
        }
      else if (errno == EBADF) /* Bad fd */
        {
          JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
          JCL_ThrowException (env, NON_READABLE_CHANNEL_EXCEPTION, 
                              strerror(errno));
          return -1;
        }
      else if (EINTR == errno) /* read interrupted */
        {
          JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
          JCL_ThrowException(env, INTERRUPTED_IO_EXCEPTION, strerror (errno));
          return -1;
        }
      else
        {
          JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
      	  JCL_ThrowException (env, IO_EXCEPTION, strerror(errno));
      	  return -1;
        }
    }
  else 
    buf.count = result;
      
  JCL_release_buffer(env, &buf, bbuf, 0);
  return result;


}


JNIEXPORT jint JNICALL 
Java_gnu_java_nio_VMChannel_write__ILjava_nio_ByteBuffer_2 (JNIEnv *env, 
                                                            jobject o __attribute__ ((__unused__)), 
                                                            jint fd, 
                                                            jobject bbuf)
{
  jint len;
  ssize_t result;
  struct JCL_buffer buf;
  int tmp_errno;

/*   NIODBG("fd: %d; bbuf: %p", fd, bbuf); */
  
  if (JCL_init_buffer(env, &buf, bbuf) < 0)
    {
      /* TODO: Rethrown exception */
      JCL_ThrowException (env, IO_EXCEPTION, "Buffer initialisation failed");
      return -1;
    }

  len = buf.limit - buf.position;

  if (len == 0)
    {
      JCL_release_buffer (env, &buf, bbuf, JNI_ABORT);
      return 0;
    }
  
  do
    {
      result = write (fd, &(buf.ptr[buf.position + buf.offset]), len);
      tmp_errno = errno;
    }
  while (result == -1 && errno == EINTR && ! JCL_thread_interrupted(env));
  errno = tmp_errno;

  buf.count = result;

  if (result == -1)
    {
      if (errno == EAGAIN) /* Non-blocking */
        {
          result = 0;
        }
      else
        {
          JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
          JCL_ThrowException(env, IO_EXCEPTION, strerror(errno));
          return -1;
        }
    }
    
  JCL_release_buffer(env, &buf, bbuf, JNI_ABORT);
  
  return result;
}

JNIEXPORT void JNICALL
Java_gnu_java_nio_VMChannel_close (JNIEnv *env,
                                   jclass c __attribute__((unused)),
                                   jint fd)
{
  if (close (fd) == -1)
    JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
}


enum FileChannel_mode {
  CPNIO_READ   = 1,
  CPNIO_WRITE  = 2,
  CPNIO_APPEND = 4,
  CPNIO_EXCL   = 8,
  CPNIO_SYNC   = 16,
  CPNIO_DSYNC  = 32
};


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL
Java_gnu_java_nio_VMChannel_open (JNIEnv *env,
                                  jclass c __attribute__((unused)),
                                  jstring path, jint mode)
{
  int nmode = 0;
  int ret;
  const char *npath;

  if ((mode & CPNIO_READ) && (mode & CPNIO_WRITE))
    nmode = O_RDWR;
  else if (mode & CPNIO_WRITE)
    nmode = O_WRONLY;
  else
    nmode = O_RDONLY;

  nmode = (nmode
           | ((nmode == O_RDWR || nmode == O_WRONLY) ? O_CREAT : 0)
           | ((mode & CPNIO_APPEND) ? O_APPEND :
              ((nmode == O_WRONLY) ? O_TRUNC : 0))
           | ((mode & CPNIO_EXCL) ? O_EXCL : 0)
           | ((mode & CPNIO_SYNC) ? O_SYNC : 0));

  npath = JCL_jstring_to_cstring (env, path);

/*   NIODBG("path: %s; mode: %x", npath, nmode); */

  ret = open (npath, nmode, 0666);

/*   NIODBG("ret: %d\n", ret); */

  JCL_free_cstring (env, path, npath);

  if (-1 == ret)
    JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));

  return ret;
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    position
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_gnu_java_nio_VMChannel_position (JNIEnv *env,
                                      jclass c __attribute__((unused)),
                                      jint fd)
{

  off_t ret;

  ret = lseek (fd, 0, SEEK_CUR);

  if (-1 == ret)
    JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));

  return (jlong) ret;

  //JCL_ThrowException (env, IO_EXCEPTION, "position not supported");
  //return -1;
/* HAVE_LSEEK */
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    seek
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL
Java_gnu_java_nio_VMChannel_seek (JNIEnv *env,
                                  jclass c __attribute__((unused)),
                                  jint fd, jlong pos)
{

  if (lseek (fd, (off_t) pos, SEEK_SET) == -1)
    JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));

  // JCL_ThrowException (env, IO_EXCEPTION, "seek not supported");
/* HAVE_LSEEK */
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    truncate
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL
Java_gnu_java_nio_VMChannel_truncate (JNIEnv *env,
                                      jclass c __attribute__((unused)),
                                      jint fd, jlong len)
{
    //#if defined(HAVE_FTRUNCATE) && defined(HAVE_LSEEK)
  off_t pos = lseek (fd, 0, SEEK_CUR);
  if (pos == -1)
    {
      JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return;
    }
  if (ftruncate (fd, (off_t) len) == -1)
    {
      JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return;
    }
  if (pos > len)
    {
      if (lseek (fd, len, SEEK_SET) == -1)
        JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
    }
  //#else
  //  JCL_ThrowException (env, IO_EXCEPTION, "truncate not supported");
  //#endif /* HAVE_FTRUNCATE && HAVE_LSEEK */
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    lock
 * Signature: (IJJZZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_gnu_java_nio_VMChannel_lock (JNIEnv *env,
                                  jclass c __attribute__((unused)),
                                  jint fd, jlong pos, jlong len,
                                  jboolean shared, jboolean wait)
{

  struct flock fl;

  fl.l_start  = (off_t) pos;
  /* Long.MAX_VALUE means lock everything possible starting at pos. */
  if (len == 9223372036854775807LL)
    fl.l_len = 0;
  else
    fl.l_len = (off_t) len;
  fl.l_pid    = getpid ();
  fl.l_type   = (shared ? F_RDLCK : F_WRLCK);
  fl.l_whence = SEEK_SET;

  if (cpnio_fcntl (fd, (wait ? F_SETLKW : F_SETLK), (long) &fl) == -1)
    {
      if (errno != EAGAIN)
        JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return JNI_FALSE;
    }

  return JNI_TRUE;

}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    unlock
 * Signature: (IJJ)V
 */
JNIEXPORT void JNICALL
Java_gnu_java_nio_VMChannel_unlock (JNIEnv *env,
                                    jclass c __attribute__((unused)),
                                    jint fd, jlong pos, jlong len)
{

  struct flock fl;

  fl.l_start  = (off_t) pos;
  fl.l_len    = (off_t) len;
  fl.l_pid    = getpid ();
  fl.l_type   = F_UNLCK;
  fl.l_whence = SEEK_SET;

  if (cpnio_fcntl (fd, F_SETLK, (long) &fl) == -1)
    {
      JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
    }

}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    size
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_gnu_java_nio_VMChannel_size (JNIEnv *env,
                                  jclass c __attribute__((unused)),
                                  jint fd)
{

  struct stat st;

  if (fstat (fd, &st) == -1)
    JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));

  return (jlong) st.st_size;
}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    map
 * Signature: (ICJI)Lgnu/classpath/Pointer;
 */
JNIEXPORT jobject JNICALL
Java_gnu_java_nio_VMChannel_map (JNIEnv *env,
                                 jclass clazz __attribute__((unused)),
                                 jint fd, jchar mode, jlong position, jint size)
{
   
    JCL_ThrowException (env, IO_EXCEPTION, "map not supported");
    return 0;
    
    /*
  jclass MappedByteBufferImpl_class;
  jmethodID MappedByteBufferImpl_init = NULL;
  jobject Pointer_instance;
  volatile jobject buffer;
  long pagesize;
  int prot, flags;
  void *p;
  void *address;


  if ((*env)->ExceptionOccurred (env))
    {
      return NULL;
    }

  prot = PROT_READ;
  if (mode == '+' || mode == 'c')
    {
      
      struct stat st;
      if (fstat (fd, &st) == -1)
        {
          JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
          return NULL;
        }
      if (position + size > st.st_size)
        {
          if (ftruncate(fd, position + size) == -1)
            {
              JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
              return NULL;
            }
        }
      prot |= PROT_WRITE;
    }

  flags = (mode == 'c' ? MAP_PRIVATE : MAP_SHARED);
  p = mmap (NULL, (size_t) ALIGN_UP (size, pagesize), prot, flags,
	    fd, ALIGN_DOWN (position, pagesize));
  if (p == MAP_FAILED)
    {
      JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return NULL;
    }

  
  address = (void *) ((char *) p + (position % pagesize));

  Pointer_instance = JCL_NewRawDataObject(env, address);

  MappedByteBufferImpl_class = (*env)->FindClass (env,
						  "java/nio/MappedByteBufferImpl");
  if (MappedByteBufferImpl_class != NULL)
    {
      MappedByteBufferImpl_init =
	(*env)->GetMethodID (env, MappedByteBufferImpl_class,
			     "<init>", "(Lgnu/classpath/Pointer;IZ)V");
    }

  if ((*env)->ExceptionOccurred (env))
    {
      munmap (p, ALIGN_UP (size, pagesize));
      return NULL;
    }
  if (MappedByteBufferImpl_init == NULL)
    {
      JCL_ThrowException (env, "java/lang/InternalError",
                          "could not get MappedByteBufferImpl constructor");
      munmap (p, ALIGN_UP (size, pagesize));
      return NULL;
    }

  buffer = (*env)->NewObject (env, MappedByteBufferImpl_class,
                              MappedByteBufferImpl_init, Pointer_instance,
                              (jint) size, mode == 'r');
  return buffer;
*/
}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    flush
 * Signature: (IZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_gnu_java_nio_VMChannel_flush (JNIEnv *env,
                                   jclass c __attribute__((unused)),
                                   jint fd, jboolean metadata __attribute__((unused)))
{

  /* XXX blocking? */
  if (fsync (fd) == -1)
    {
      JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return JNI_FALSE;
    }
  return JNI_TRUE;

}

/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    read
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_gnu_java_nio_VMChannel_read__I (JNIEnv *env,
                                     jclass c __attribute__((unused)),
                                     jint fd)
{
  char in;
  int ret;
  int tmp_errno;

/*   NIODBG("fd: %d", fd); */
  do
    {
      ret = cpnio_read (fd, &in, 1);
      tmp_errno = errno;
      
    }
  while (ret == -1 && errno == EINTR && ! JCL_thread_interrupted(env));
  errno = tmp_errno;

  if (-1 == ret)
    {
      if (errno == EAGAIN && !is_non_blocking_fd(fd))
        {
          /* Read timeout on a socket with SO_RCVTIMEO != 0. */
          JCL_ThrowException(env, SOCKET_TIMEOUT_EXCEPTION, "read timed out");
        }
      else
        JCL_ThrowException (env, IO_EXCEPTION, strerror (errno));
      return -1;
    }
  
  if (0 == ret){
      return -1;
  }
 
  return (in & 0xFF);
}


/*
 * Class:     gnu_java_nio_VMChannel
 * Method:    write
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_gnu_java_nio_VMChannel_write__II (JNIEnv *env,
                                       jclass c __attribute__((unused)),
                                       jint fd, jint data)
{

  char out = (char) data;
  int ret;
  int tmp_errno;

/*   NIODBG("fd: %d; data: %d", fd, data); */

  do
    {
      ret = cpnio_write (fd, &out, 1);
      tmp_errno = errno;
    }
  while (ret == -1 && errno == EINTR && ! JCL_thread_interrupted(env));
  errno = tmp_errno;

  if (-1 == ret)
    JCL_ThrowException(env, IO_EXCEPTION, strerror (errno));

  
}




#ifdef __cplusplus
}
#endif


static JNINativeMethod natives[] = {
    { "initIDs", "()V", Java_gnu_java_nio_VMChannel_initIDs },
    { "stdin_fd", "()I", Java_gnu_java_nio_VMChannel_stdin_1fd },
    { "stdout_fd", "()I", Java_gnu_java_nio_VMChannel_stdout_1fd },
    { "stderr_fd", "()I", Java_gnu_java_nio_VMChannel_stderr_1fd },
    { "write", "(ILjava/nio/ByteBuffer;)I", Java_gnu_java_nio_VMChannel_write__ILjava_nio_ByteBuffer_2 },
    { "read", "(I)I", Java_gnu_java_nio_VMChannel_read__I },
    { "write", "(I)V", Java_gnu_java_nio_VMChannel_write__II },
    { "read", "(ILjava/nio/ByteBuffer;)I", Java_gnu_java_nio_VMChannel_read__ILjava_nio_ByteBuffer_2 },
    { "open", "(Ljava/lang/String;I)I", Java_gnu_java_nio_VMChannel_open },
    { "close", "(I)V", Java_gnu_java_nio_VMChannel_close},
    { "position", "(I)J", Java_gnu_java_nio_VMChannel_position },
    { "seek", "(IJ)V", Java_gnu_java_nio_VMChannel_seek },
    { "truncate", "(IJ)V", Java_gnu_java_nio_VMChannel_truncate },
    { "lock", "(IJJZZ)Z", Java_gnu_java_nio_VMChannel_lock },
    { "unlock", "(IJJ)V", Java_gnu_java_nio_VMChannel_unlock },
    { "size", "(I)J", Java_gnu_java_nio_VMChannel_size },
    { "map", "(ICJI)Lgnu/classpath/Pointer;", Java_gnu_java_nio_VMChannel_map },
    { "flush", "(IZ)Z", Java_gnu_java_nio_VMChannel_flush }
  
};

jint VMChannel_OnLoad(JavaVM *vm, void *arg) {
  JNIEnv *env;

  if ((*vm)->GetEnv (vm, &env, JNI_VERSION_1_4) != JNI_OK)
    {
      return JNI_VERSION_1_4;
    }

  (*env)->RegisterNatives(env,JCL_FindClass(env,"gnu/java/nio/VMChannel"),
			  natives,sizeof(natives)/sizeof(JNINativeMethod));
  
  return JNI_VERSION_1_4;
}



