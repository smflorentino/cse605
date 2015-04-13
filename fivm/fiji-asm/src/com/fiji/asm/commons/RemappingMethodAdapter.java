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

package com.fiji.asm.commons;

import com.fiji.asm.AnnotationVisitor;
import com.fiji.asm.Label;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.UTF8Sequence;

/**
 * A <code>MethodAdapter</code> for type mapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingMethodAdapter extends LocalVariablesSorter {

    protected final Remapper remapper;

    public RemappingMethodAdapter(
        int access,
        String desc,
        MethodVisitor mv,
        Remapper renamer)
    {
        super(access, desc, mv);
        this.remapper = renamer;
    }

    public void visitFieldInsn(
        int opcode,
        UTF8Sequence owner,
        UTF8Sequence name,
        UTF8Sequence desc)
    {
        super.visitFieldInsn(opcode,
                remapper.mapType(owner),
                new UTF8Sequence(remapper.mapFieldName(owner.toString(), name.toString(), desc.toString())),
                remapper.mapDesc(desc));
    }

    public void visitMethodInsn(
        int opcode,
        UTF8Sequence owner,
        UTF8Sequence name,
        UTF8Sequence desc)
    {
        super.visitMethodInsn(opcode,
                remapper.mapType(owner),
                new UTF8Sequence(remapper.mapMethodName(owner.toString(), name.toString(), desc.toString())),
                remapper.mapMethodDesc(desc));
    }

    public void visitTypeInsn(int opcode, UTF8Sequence type) {
        super.visitTypeInsn(opcode, remapper.mapType(type));
    }

    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(remapper.mapValue(cst));
    }

    public void visitMultiANewArrayInsn(UTF8Sequence desc, int dims) {
        super.visitMultiANewArrayInsn(remapper.mapDesc(desc), dims);
    }

    public void visitTryCatchBlock(
        Label start,
        Label end,
        Label handler,
        UTF8Sequence type)
    {
        super.visitTryCatchBlock(start, end, handler, // 
                type == null ? null : remapper.mapType(type));
    }
    
    public void visitLocalVariable(
        UTF8Sequence name,
        UTF8Sequence desc,
        UTF8Sequence signature,
        Label start,
        Label end,
        int index)
    {
        super.visitLocalVariable(name,
                remapper.mapDesc(desc),
                new UTF8Sequence(remapper.mapSignature(signature.toString(), true)),
                start,
                end,
                index);
    }

    public AnnotationVisitor visitAnnotation(UTF8Sequence desc, boolean visible) {
        AnnotationVisitor av = mv.visitAnnotation(desc, visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor av = mv.visitAnnotationDefault();
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public AnnotationVisitor visitParameterAnnotation(
        int parameter,
        UTF8Sequence desc,
        boolean visible)
    {
        AnnotationVisitor av = mv.visitParameterAnnotation(parameter,
                desc,
                visible);
        return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
    }
    
    public void visitFrame(
        int type,
        int nLocal,
        Object[] local,
        int nStack,
        Object[] stack)
    {
        super.visitFrame(type, nLocal, remapEntries(nLocal, local), nStack, remapEntries(nStack, stack));
    }

    private Object[] remapEntries(int n, Object[] entries) {
        for (int i = 0; i < n; i++) {
            if (entries[i] instanceof String) {
                Object[] newEntries = new Object[n];
                if (i > 0) {
                    System.arraycopy(entries, 0, newEntries, 0, i);
                }
                do {
                    Object t = entries[i];
                    newEntries[i++] = t instanceof String
                            ? remapper.mapType((String) t)
                            : t;
                } while (i < n);
                return newEntries;
            }
        }
        return entries;
    }
    
}
