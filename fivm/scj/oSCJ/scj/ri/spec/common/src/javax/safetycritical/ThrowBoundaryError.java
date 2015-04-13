/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;

import javax.safetycritical.annotate.SCJRestricted;

/**
 * One ThrowBoundaryError is preallocated for each Schedulable in its outer-most
 * private scope.
 */
@SCJAllowed
public class ThrowBoundaryError extends javax.realtime.ThrowBoundaryError {

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public ThrowBoundaryError() {
	}

	/**
	 * Shall not copy "this" to any instance or static field.
	 * <p>
	 * Allocates and returns a String object and its backing store to represent
	 * the message associated with the thrown exception that most recently
	 * crossed a scope boundary within this thread.
	 * <p>
	 * For each \texttt{Schedulable}, a single shared \texttt{String\-Builder}
	 * represents the stack back trace method and class names for the most
	 * recently constructed \texttt{Throwable}, and the message for the
	 * \texttt{Throwable} that most recently crossed a scope boundary. The
	 * \texttt{get\-Propagated\-Message} method copies data out of this shared
	 * \texttt{StringBuilder} object.
	 * <p>
	 * The original message is truncated if it is longer than the length of the
	 * thread-local \texttt{StringBuilder} object, which length is specified in
	 * the \texttt{Storage\-Con\-fig\-ura\-tion\-Pa\-ra\-meters} for this
	 * \texttt{Schedulable}.
	 */
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public String getPropagatedMessage() {
		return null;
	}

	/**
	 * Shall not copy "this" to any instance or static field.
	 * <p>
	 * Allocates a StackTraceElement array, StackTraceElement objects, and all
	 * internal structure, including String objects referenced from each
	 * StackTraceElement to represent the stack backtrace information available
	 * for the exception that was most recently associated with this
	 * ThrowBoundaryError object.
	 * <p>
	 * Each Schedulable maintains a single thread-local buffer to represent the
	 * stack back trace information associated with the most recent invocation
	 * of System.captureStackBacktrace(). The size of this buffer is specified
	 * by providing a StorageParameters object as an argument to construction of
	 * the Schedulable. Most commonly, System.captureStackBacktrace() is invoked
	 * from within the constructor of java.lang.Throwable.
	 * getPropagatedStackTrace() returns a representation of this thread-local
	 * back trace information. Under normal circumstances, this stack back trace
	 * information corresponds to the exception represented by this
	 * ThrowBoundaryError object. However, certain execution sequences may
	 * overwrite the contents of the buffer so that the stack back trace
	 * information so that the stack back trace information is not relevant.
	 */
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public StackTraceElement[] getPropagatedStackTrace() {
		return null;
	}

	/**
	 * Performs no allocation. Shall not copy "this" to any instance or static
	 * field.
	 * <p>
	 * Returns the number of valid elements stored within the StackTraceElement
	 * array to be returned by getPropagatedStackTrace().
	 */
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
	public int getPropagatedStackTraceDepth() {
		return 0;
	}

	/**
	 * Performs no allocation. Shall not copy "this" to any instance or static
	 * field.
	 * <p>
	 * Returns a reference to the Class of the exception most recently thrown
	 * across a scope boundary by the current thread.
	 */
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
	public Class getPropagatedExceptionClass() {
		return null;
	}
}
