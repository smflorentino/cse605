/*
 * Protocols.java
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

package com.fiji.fivm.codegen;

import com.fiji.asm.Opcodes;
import com.fiji.fivm.Constants;
import com.fiji.fivm.Settings;
import com.fiji.fivm.r1.Magic;

public final class Protocols {
    private Protocols() {}
    
    public static int opcodeForFat(int fat) {
        switch (fat) {
        case Constants.FAT_PUTFIELD: return Opcodes.PUTFIELD;
        case Constants.FAT_GETFIELD: return Opcodes.GETFIELD;
        case Constants.FAT_PUTSTATIC: return Opcodes.PUTSTATIC;
        case Constants.FAT_GETSTATIC: return Opcodes.GETSTATIC;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid fat: "+fat);
            }
            throw Magic.notReached();
        }
    }
    
    public static int fatForOpcode(int opcode) {
        switch (opcode) {
        case Opcodes.PUTFIELD: return Constants.FAT_PUTFIELD;
        case Opcodes.GETFIELD: return Constants.FAT_GETFIELD;
        case Opcodes.PUTSTATIC: return Constants.FAT_PUTSTATIC;
        case Opcodes.GETSTATIC: return Constants.FAT_GETSTATIC;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid opcode: "+opcode);
            }
            throw Magic.notReached();
        }
    }
    
    public static int opcodeForMct(int mct) {
        switch (mct) {
        case Constants.MCT_INVOKESTATIC: return Opcodes.INVOKESTATIC;
        case Constants.MCT_INVOKEVIRTUAL: return Opcodes.INVOKEVIRTUAL;
        case Constants.MCT_INVOKEINTERFACE: return Opcodes.INVOKEINTERFACE;
        case Constants.MCT_INVOKESPECIAL: return Opcodes.INVOKESPECIAL;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid mct: "+mct);
            }
            throw Magic.notReached();
        }
    }
    
    public static int mctForOpcode(int opcode) {
        switch (opcode) {
        case Opcodes.INVOKESTATIC: return Constants.MCT_INVOKESTATIC;
        case Opcodes.INVOKEVIRTUAL: return Constants.MCT_INVOKEVIRTUAL;
        case Opcodes.INVOKEINTERFACE: return Constants.MCT_INVOKEINTERFACE;
        case Opcodes.INVOKESPECIAL: return Constants.MCT_INVOKESPECIAL;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid opcode: "+opcode);
            }
            throw Magic.notReached();
        }
    }
    
    public static int opcodeForIot(int iot) {
        switch (iot) {
        case Constants.IOT_INSTANCEOF: return Opcodes.INSTANCEOF;
        case Constants.IOT_CHECKCAST: return Opcodes.CHECKCAST;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid iot: "+iot);
            }
            throw Magic.notReached();
        }
    }
    
    public static int iotForOpcode(int opcode) {
        switch (opcode) {
        case Opcodes.INSTANCEOF: return Constants.IOT_INSTANCEOF;
        case Opcodes.CHECKCAST: return Constants.IOT_CHECKCAST;
        default:
            if (Settings.ASSERTS_ON) {
                throw new Error("Invalid opcode: "+opcode);
            }
            throw Magic.notReached();
        }
    }
    
    public static boolean isInstance(int opcode) {
        switch (opcode) {
        case Opcodes.PUTFIELD:
        case Opcodes.GETFIELD:
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKEINTERFACE:
            return true;
        default:
            return false;
        }
    }
    
    public static boolean isStatic(int opcode) {
        return !isInstance(opcode);
    }
    
    public static int recvSlot(int opcode,
                               char type) {
        switch (opcode) {
        case Opcodes.PUTFIELD:
            return Types.cells(type);
        default:
            return 0;
        }
    }
}

