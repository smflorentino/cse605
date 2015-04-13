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
import com.fiji.asm.ClassAdapter;
import com.fiji.asm.ClassVisitor;
import com.fiji.asm.FieldVisitor;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.UTF8Sequence;

/**
 * A <code>ClassAdapter</code> for type remapping.
 * 
 * @author Eugene Kuleshov
 */
public class RemappingClassAdapter extends ClassAdapter {

    protected final Remapper remapper;
    
    protected String className;

    public RemappingClassAdapter(ClassVisitor cv, Remapper remapper) {
        super(cv);
        this.remapper = remapper;
    }

    public void visit(
        int version,
        int access,
        UTF8Sequence name,
        UTF8Sequence signature,
        UTF8Sequence superName,
        UTF8Sequence[] interfaces)
    {
        this.className = name.toString();
        if (interfaces!=null) {
            String[] interfaces_=new String[interfaces.length];
            for (int i=0;i<interfaces.length;++i) interfaces_[i]=interfaces[i].toString();
            interfaces_=remapper.mapTypes(interfaces_);
            interfaces=new UTF8Sequence[interfaces.length];
            for (int i=0;i<interfaces.length;++i) interfaces[i]=new UTF8Sequence(interfaces_[i]);
        }
        super.visit(version,
                access,
                new UTF8Sequence(remapper.mapType(name.toString())),
                new UTF8Sequence(remapper.mapSignature(signature.toString(), false)),
                new UTF8Sequence(remapper.mapType(superName.toString())),
                interfaces);
    }

    public AnnotationVisitor visitAnnotation(UTF8Sequence desc, boolean visible) {
        AnnotationVisitor av;
        av = super.visitAnnotation(new UTF8Sequence(remapper.mapType(desc.toString())), visible);
        return av == null ? null : createRemappingAnnotationAdapter(av);
    }

    public FieldVisitor visitField(
        int access,
        UTF8Sequence name,
        UTF8Sequence desc,
        UTF8Sequence signature,
        Object value)
    {
        FieldVisitor fv = super.visitField(access,
                new UTF8Sequence(remapper.mapFieldName(className, name.toString(), desc.toString())),
                new UTF8Sequence(remapper.mapDesc(desc.toString())),
                new UTF8Sequence(remapper.mapSignature(signature.toString(), true)),
                remapper.mapValue(value));
        return fv == null ? null : createRemappingFieldAdapter(fv);
    }

    public MethodVisitor visitMethod(
        int access,
        UTF8Sequence name,
        UTF8Sequence desc,
        UTF8Sequence signature,
        UTF8Sequence[] exceptions)
    {
        String newDesc = remapper.mapMethodDesc(desc.toString());
        MethodVisitor mv = super.visitMethod(access,
                new UTF8Sequence(remapper.mapMethodName(className, name.toString(), desc.toString())),
                new UTF8Sequence(newDesc),
                new UTF8Sequence(remapper.mapSignature(signature.toString(), false)),
                exceptions == null ? null : remapper.mapTypes(exceptions));
        return mv == null ? null : createRemappingMethodAdapter(access, newDesc, mv);
    }

    public void visitInnerClass(
        UTF8Sequence name,
        UTF8Sequence outerName,
        UTF8Sequence innerName,
        int access)
    {
        super.visitInnerClass(remapper.mapType(name),
                outerName == null ? null : remapper.mapType(outerName),
                innerName, // TODO should it be changed?
                access);
    }

    public void visitOuterClass(UTF8Sequence owner, UTF8Sequence name, UTF8Sequence desc) {
        super.visitOuterClass(remapper.mapType(owner), 
                name == null ? null : new UTF8Sequence(remapper.mapMethodName(owner.toString(), name.toString(), desc.toString())), 
                desc == null ? null : new UTF8Sequence(remapper.mapMethodDesc(desc.toString())));
    }

    protected FieldVisitor createRemappingFieldAdapter(FieldVisitor fv) {
        return new RemappingFieldAdapter(fv, remapper);
    }
    
    protected MethodVisitor createRemappingMethodAdapter(
        int access,
        String newDesc,
        MethodVisitor mv)
    {
        return new RemappingMethodAdapter(access, newDesc, mv, remapper);
    }

    protected AnnotationVisitor createRemappingAnnotationAdapter(
        AnnotationVisitor av)
    {
        return new RemappingAnnotationAdapter(av, remapper);
    }
}
