;
; TableSwitchTest5.j
; Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
; This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
; LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
; available at fivm/LEGAL and can also be found at
; http://www.fiji-systems.com/FPL3.txt
; 
; By installing, reproducing, distributing, and/or using the FIJI VM Software
; you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
; rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
; restrictions stated therein.  Among other conditions and restrictions, the
; FIJI PUBLIC LICENSE states that:
; 
; a. You may only make non-commercial use of the FIJI VM Software.
; 
; b. Any adaptation you make must be licensed under the same terms 
; of the FIJI PUBLIC LICENSE.
; 
; c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
; file, adaptation or output code that you distribute and cause the output code
; to provide a notice of the FIJI PUBLIC LICENSE. 
; 
; d. You must not impose any additional conditions.
; 
; e. You must not assert or imply any connection, sponsorship or endorsement by
; the author of the FIJI VM Software
; 
; f. You must take no derogatory action in relation to the FIJI VM Software
; which would be prejudicial to the FIJI VM Software author's honor or
; reputation.
; 
; 
; The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
; representation and provides no warranty of any kind concerning the software.
; 
; The FIJI PUBLIC LICENSE and any rights granted therein terminate
; automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
;
;

.source TableSwitchTest5.j
.class public com/fiji/fivm/test/TableSwitchTest5
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
.limit stack 2
.limit locals 1
       aload_0
       iconst_0
       aaload
       invokestatic java/lang/Integer/parseInt(Ljava/lang/String;)I
       tableswitch -15
           L0
           L1
           L2
           L3
           L4
           L5
           L6
           L7
           L8
           L9
           default: Ldefault
L0:
       ldc 1000
       goto done
L1:
       ldc 1100
       goto done
L2:
       ldc 1200
       goto done
L3:
       ldc 1300
       goto done
L4:
       ldc 1400
       goto done
L5:
       ldc 1500
       goto done
L6:
       ldc 1600
       goto done
L7:
       ldc 1700
       goto done
L8:
       ldc 1800
       goto done
L9:
       ldc 1900
       goto done
Ldefault:
       ldc 2000
done:
       getstatic java/lang/System/out Ljava/io/PrintStream;
       swap
       invokevirtual java/io/PrintStream/println(I)V
       return
.end method

