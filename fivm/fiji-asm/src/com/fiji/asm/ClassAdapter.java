/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fiji.asm;

/**
 * An empty {@link ClassVisitor} that delegates to another {@link ClassVisitor}.
 * This class can be used as a super class to quickly implement usefull class
 * adapter classes, just by overriding the necessary methods.
 * 
 * @author Eric Bruneton
 */
public class ClassAdapter implements ClassVisitor {

    /**
     * The {@link ClassVisitor} to which this adapter delegates calls.
     */
    protected ClassVisitor cv;

    /**
     * Constructs a new {@link ClassAdapter} object.
     * 
     * @param cv the class visitor to which this adapter must delegate calls.
     */
    public ClassAdapter(final ClassVisitor cv) {
        this.cv = cv;
    }

    public void visit(
        final int version,
        final int access,
        final UTF8Sequence name,
        final UTF8Sequence signature,
        final UTF8Sequence superName,
        final UTF8Sequence[] interfaces)
    {
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    public void visitSource(final UTF8Sequence source, final UTF8Sequence debug) {
        cv.visitSource(source, debug);
    }

    public void visitOuterClass(
        final UTF8Sequence owner,
        final UTF8Sequence name,
        final UTF8Sequence desc)
    {
        cv.visitOuterClass(owner, name, desc);
    }

    public AnnotationVisitor visitAnnotation(
        final UTF8Sequence desc,
        final boolean visible)
    {
        return cv.visitAnnotation(desc, visible);
    }

    public void visitAttribute(final Attribute attr) {
        cv.visitAttribute(attr);
    }

    public void visitInnerClass(
        final UTF8Sequence name,
        final UTF8Sequence outerName,
        final UTF8Sequence innerName,
        final int access)
    {
        cv.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(
        final int access,
        final UTF8Sequence name,
        final UTF8Sequence desc,
        final UTF8Sequence signature,
        final Object value)
    {
        return cv.visitField(access, name, desc, signature, value);
    }

    public MethodVisitor visitMethod(
        final int access,
        final UTF8Sequence name,
        final UTF8Sequence desc,
        final UTF8Sequence signature,
        final UTF8Sequence[] exceptions)
    {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    public void visitEnd() {
        cv.visitEnd();
    }
}
