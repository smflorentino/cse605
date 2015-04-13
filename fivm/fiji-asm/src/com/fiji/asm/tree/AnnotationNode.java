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
package com.fiji.asm.tree;

import java.util.ArrayList;
import java.util.List;

import com.fiji.asm.AnnotationVisitor;
import com.fiji.asm.UTF8Sequence;

/**
 * A node that represents an annotationn.
 * 
 * @author Eric Bruneton
 */
public class AnnotationNode implements AnnotationVisitor {

    /**
     * The class descriptor of the annotation class.
     */
    public String desc;

    /**
     * The name value pairs of this annotation. Each name value pair is stored
     * as two consecutive elements in the list. The name is a {@link String},
     * and the value may be a {@link Byte}, {@link Boolean}, {@link Character},
     * {@link Short}, {@link Integer}, {@link Long}, {@link Float},
     * {@link Double}, {@link String} or {@link com.fiji.asm.Type}, or an
     * two elements String array (for enumeration values), a
     * {@link AnnotationNode}, or a {@link List} of values of one of the
     * preceding types. The list may be <tt>null</tt> if there is no name
     * value pair.
     */
    public List values;

    /**
     * Constructs a new {@link AnnotationNode}.
     * 
     * @param desc the class descriptor of the annotation class.
     */
    public AnnotationNode(final String desc) {
        this.desc = desc;
    }

    /**
     * Constructs a new {@link AnnotationNode} to visit an array value.
     * 
     * @param values where the visited values must be stored.
     */
    AnnotationNode(final List values) {
        this.values = values;
    }

    // ------------------------------------------------------------------------
    // Implementation of the AnnotationVisitor interface
    // ------------------------------------------------------------------------

    public void visit(final UTF8Sequence name, final Object value) {
        if (values == null) {
            values = new ArrayList(this.desc != null ? 2 : 1);
        }
        if (this.desc != null) {
            values.add(name.toString());
        }
        values.add(value);
    }

    public void visitEnum(
        final UTF8Sequence name,
        final UTF8Sequence desc,
        final UTF8Sequence value)
    {
        if (values == null) {
            values = new ArrayList(this.desc != null ? 2 : 1);
        }
        if (this.desc != null) {
            values.add(name.toString());
        }
        values.add(new String[] { desc.toString(), value.toString() });
    }

    public AnnotationVisitor visitAnnotation(
        final UTF8Sequence name,
        final UTF8Sequence desc)
    {
        if (values == null) {
            values = new ArrayList(this.desc != null ? 2 : 1);
        }
        if (this.desc != null) {
            values.add(name.toString());
        }
        AnnotationNode annotation = new AnnotationNode(desc.toString());
        values.add(annotation);
        return annotation;
    }

    public AnnotationVisitor visitArray(final UTF8Sequence name) {
        if (values == null) {
            values = new ArrayList(this.desc != null ? 2 : 1);
        }
        if (this.desc != null) {
            values.add(name.toString());
        }
        List array = new ArrayList();
        values.add(array);
        return new AnnotationNode(array);
    }

    public void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Accept methods
    // ------------------------------------------------------------------------

    /**
     * Makes the given visitor visit this annotation.
     * 
     * @param av an annotation visitor. Maybe <tt>null</tt>.
     */
    public void accept(final AnnotationVisitor av) {
        if (av != null) {
            if (values != null) {
                for (int i = 0; i < values.size(); i += 2) {
                    String name = (String) values.get(i);
                    Object value = values.get(i + 1);
                    accept(av, name, value);
                }
            }
            av.visitEnd();
        }
    }

    /**
     * Makes the given visitor visit a given annotation value.
     * 
     * @param av an annotation visitor. Maybe <tt>null</tt>.
     * @param name the value name.
     * @param value the actual value.
     */
    static void accept(
        final AnnotationVisitor av,
        final String name,
        final Object value)
    {
        if (av != null) {
            if (value instanceof String[]) {
                String[] typeconst = (String[]) value;
                av.visitEnum(new UTF8Sequence(name), new UTF8Sequence(typeconst[0]), new UTF8Sequence(typeconst[1]));
            } else if (value instanceof AnnotationNode) {
                AnnotationNode an = (AnnotationNode) value;
                an.accept(av.visitAnnotation(new UTF8Sequence(name), new UTF8Sequence(an.desc)));
            } else if (value instanceof List) {
                AnnotationVisitor v = av.visitArray(new UTF8Sequence(name));
                List array = (List) value;
                for (int j = 0; j < array.size(); ++j) {
                    accept(v, null, array.get(j));
                }
                v.visitEnd();
            } else {
                av.visit(new UTF8Sequence(name), value);
            }
        }
    }
}
