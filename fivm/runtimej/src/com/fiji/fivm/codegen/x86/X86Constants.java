/*
 * X86Constants.java
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

package com.fiji.fivm.codegen.x86;

import com.fiji.fivm.r1.*;

/**
 * @author Hiroshi Yamauchi
 **/
public final class X86Constants {

    public final static byte R_none = -1;
    public final static byte R_AX = 0;
    public final static byte R_CX = 1;
    public final static byte R_DX = 2;
    public final static byte R_BX = 3;
    public final static byte R_SP = 4;
    public final static byte R_BP = 5;
    public final static byte R_SI = 6;
    public final static byte R_DI = 7;

    public final static int MOD_R    = 0xC0;
    public final static int MOD_M_32 = 0x80;
    public final static int MOD_M_8  = 0x40;
    public final static int MOD_M    = 0x0;

    public final static int SS_I_1 = 0x0;
    public final static int SS_I_2 = 0x40;
    public final static int SS_I_4 = 0x80;
    public final static int SS_I_8 = 0xC0;
    
    public static int scale(int mult) {
        switch (mult) {
        case 1:
            return SS_I_1;
        case 2:
            return SS_I_2;
        case 4:
            return SS_I_4;
        case 8:
            return SS_I_8;
        default:
            throw new fivmError("Bad multiplier: "+mult);
        }
    }

    // The parameters to the conditional jump instruction
    public final static byte J_L   = 0x0C;
    public final static byte J_LE  = 0x0E;
    public final static byte J_G   = 0x0F;
    public final static byte J_GE  = 0x0D;
    public final static byte J_BE  = 0x06;
    public final static byte J_NBE = 0x07;
    public final static byte J_AE  = 0x03;
    public final static byte J_B   = 0x02;
    public final static byte J_E   = 0x04;
    public final static byte J_NE  = 0x05;
    public final static byte J_A   = 0x07;
    public final static byte J_P   = 0x0A;
    public final static byte J_NP  = 0x0B;
    public final static byte J_C   = 0x02;
    public final static byte J_NC  = 0x03;
    public final static byte J_Z   = 0x04;
    public final static byte J_NZ  = 0x05;

}
