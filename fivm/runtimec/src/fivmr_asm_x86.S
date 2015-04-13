#
# fivmr_asm_x86.S
# Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
# This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
# LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
# available at fivm/LEGAL and can also be found at
# http://www.fiji-systems.com/FPL3.txt
# 
# By installing, reproducing, distributing, and/or using the FIJI VM Software
# you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
# rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
# restrictions stated therein.  Among other conditions and restrictions, the
# FIJI PUBLIC LICENSE states that:
# 
# a. You may only make non-commercial use of the FIJI VM Software.
# 
# b. Any adaptation you make must be licensed under the same terms 
# of the FIJI PUBLIC LICENSE.
# 
# c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
# file, adaptation or output code that you distribute and cause the output code
# to provide a notice of the FIJI PUBLIC LICENSE. 
# 
# d. You must not impose any additional conditions.
# 
# e. You must not assert or imply any connection, sponsorship or endorsement by
# the author of the FIJI VM Software
# 
# f. You must take no derogatory action in relation to the FIJI VM Software
# which would be prejudicial to the FIJI VM Software author's honor or
# reputation.
# 
# 
# The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
# representation and provides no warranty of any kind concerning the software.
# 
# The FIJI PUBLIC LICENSE and any rights granted therein terminate
# automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
#

#include <fivmr_asm.h>
	.file	"fivmr_asm_x86.S"
	.text
        

.globl FIVMR_SYMBOL(fivmr_upcallImpl)
        
        # C signature:
        # 
        # int64_t fivmr_upcallImpl(void *func, char retType, char *argTypes, int64_t *args)
        # 
        # all return values are returned as a 64-bit int.  if it was originally
        # a 32-bit int, then the high 32 bits are zeroed.  if it was originally
        # a 32-bit float then it's converted to a 32-bit int by fiat and the
        # high 64 bits are zeroed.  if it was originally a 64-bit float then
        # it's converted to a 64-bit int by fiat.
        #
        # note that we expect that the argument list contains no garbage.  for
        # example if there is a 16-bit integer in there somewhere, we expect that
        # the high 48 bits are zero.
FIVMR_SYMBOL(fivmr_upcallImpl):
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        subl $24, %esp
        movl %edi, (%esp)        # save registers.  FIXME: this isn't the way gdb expects
        movl %ebx, 4(%esp)
        movl %esi, 8(%esp)
        movl 12(%ebp), %edi      # edi = return type descriptor
        movl 16(%ebp), %edx      # edx = null-terminated argument type descriptors
        movl 20(%ebp), %ebx      # ebx = 64-bit value list

        # what follows is a surprisingly complicated piece of code for a ridiculously
        # simple task.  what we start with is an array of 64-bit values that store
        # the arguments, and what we do is perform a call with those arguments placed
        # in reverse on the stack and packed according to cdecl conventions.  to do
        # this we first compute how big the packed size will be, then we adjust the
        # stack to accomodate that size, and finally we copy the arguments from the
        # 64-bit value array into their positions on the stack.
        
        # loop to determine the size of the arguments according to the x86 cdecl
        # calling convention.  the result will be in esi.
        xorl %esi, %esi          # esi = will have size of arguments
        movl %edx, %ecx          # ecx = current type descriptor ptr
.LargSizeLoop:
        movzbl (%ecx), %eax
        addl $1, %ecx
        cmpb $0, %al
        je .LargsSized
        cmpb $68, %al  # 68 = 'D'
        je .L64bitSize
        cmpb $74, %al  # 74 = 'J'
        je .L64bitSize
.L32bitSize:
        addl $4, %esi
        jmp .LargSizeLoop
.L64bitSize:
        addl $8, %esi
        jmp .LargSizeLoop

        # now esi has the size of arguments and ecx is dead.  set up the
        # stack pointer (esp) to make room for the arguments.
.LargsSized:                     
        addl $15, %esi
        andl $-16, %esi         # now esi has rounded up (aligned) size
        subl %esi, %esp         # stack height set up

        # loop over the arguments, placing them onto the stack in reverse (i.e. up
        # rather than down).  this will trash the argument list pointer (ebx), the
        # argument type list pointer (edx), and ecx.
        movl %esp, %ecx         # ecx will count argument positions in reverse (up not down)
.LargBuildLoop:
        movzbl (%edx), %eax
        addl $1, %edx
        cmpb $0, %al
        je .LargsBuilt
        cmpb $68, %al  # 68 = 'D'
        je .L64bitArg
        cmpb $74, %al  # 74 = 'J'
        je .L64bitArg
.L32bitArg:
        movl (%ebx), %eax
        movl %eax, (%ecx)
        addl $4, %ecx
        jmp .LincValueList
.L64bitArg:
        movl (%ebx), %eax
        movl %eax, (%ecx)
        movl 4(%ebx), %eax
        movl %eax, 4(%ecx)
        addl $8, %ecx
.LincValueList:
        addl $8, %ebx
        jmp .LargBuildLoop

        # we are ready to make the call so do it.  this will do the call and then
        # pop the arguments off the stack.
.LargsBuilt:                     # now the stack is set up for the call
        call *8(%ebp)           # make the call
        addl %esi, %esp         # return the stack to where it was prior to call

        # appropriately fudge the return value so that eax:edx holds the 64-bit
        # integer version of the value
        cmpl $70, %edi  # 70 = 'F'
        jne .LnotFloatReturn
.LfloatReturn:
        subl $4, %esp
        fstps (%esp)
        popl %eax
        xorl %edx,%edx
        jmp .LdoReturn
.LnotFloatReturn:
        cmpl $68, %edi  # 68 = 'D'
        jne .LnotDoubleReturn
.LdoubleReturn:
        subl $8, %esp
        fstpl (%esp)
        popl %eax
        popl %edx
        jmp .LdoReturn
.LnotDoubleReturn:
        cmpl $86, %edi  # 86 = 'V'
        jne .LnotVoidReturn
.LvoidReturn:
        xorl %eax,%eax
        xorl %edx,%edx
        jmp .LdoReturn
.LnotVoidReturn:
        cmpl $74, %edi  # 74 = 'J'
        je .LdoReturn           # for long return, we don't have to do anything
.LintReturn:
        xorl %edx,%edx
        jmp .LdoReturn

        # actually return
.LdoReturn:
        movl (%esp), %edi
        movl 4(%esp), %ebx
        movl 8(%esp), %esi
        leave
        ret

        
# FIXME: the nonsense that follows really needs to be modularized.
        

.globl FIVMR_SYMBOL(fivmr_resolveFieldAccessThunk)
        
        # This is not quite a cdecl-compatible function.  It gets called with one argument,
        # on a misaligned stack.  Its job is to realign the stack and then call out
        # via %esi to a field resolver.
FIVMR_SYMBOL(fivmr_resolveFieldAccessThunk):
        popl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        popl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseFieldAccess *bfa)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *(%edx)             # call fivmr_resolveField

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError1
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError1:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the jump
        subl $13, 4(%ebp)

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_resolveMethodCallThunk)

        # Resolve method call
FIVMR_SYMBOL(fivmr_resolveMethodCallThunk):
        popl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        popl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseMethodCall *bmc)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *4(%edx)            # call fivmr_resolveMethod

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError5
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError5:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the jump
        subl $13, 4(%ebp)

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_resolveInvokeInterfaceThunk)

        # Resolve interface method call (INVOKEINTERFACE opcode)
FIVMR_SYMBOL(fivmr_resolveInvokeInterfaceThunk):
        # the stack contains (in order of pop):
        # 1) real return address
        # 2) fivmr_BaseMethodCall*
        # 3) dummy return address
        #
        # and we want it to contain just the real return address,
        # with the fivmr_BaseMethodCall* saved.  and if we use eax/ecx/edx
        # then we need to save/restore them.
        movl %eax, FIVMR_OFFSETOF_REGSAVE_0(%esi)
        movl %edx, FIVMR_OFFSETOF_REGSAVE_1(%esi)
        popl %eax                # eax = real return address
        popl %edx                # edx = fivmr_BaseMethodCall*
        movl %eax, (%esp)

        # now edx contains fivmr_BaseMethodCall*, eax is dead, and the top
        # of the stack contains the real return address.  so restore eax/edx
        # while making sure that we save the fivmr_BaseMethodCall*
        movl FIVMR_OFFSETOF_REGSAVE_0(%esi), %eax
        xchgl %edx, FIVMR_OFFSETOF_REGSAVE_1(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseMethodCall *bmc)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *4(%edx)            # call fivmr_resolveMethod

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError7
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError7:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the call
        subl $15, 4(%ebp)

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_resolveArrayAllocThunk)

        # Resolve array allocation
FIVMR_SYMBOL(fivmr_resolveArrayAllocThunk):
        popl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        popl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseArrayAlloc *bmc)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *24(%edx)            # call fivmr_resolveArrayAlloc

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError9
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError9:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the jump
        subl $13, 4(%ebp) # is this right?  dunno yet.

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_resolveObjectAllocThunk)

        # Resolve object allocation
FIVMR_SYMBOL(fivmr_resolveObjectAllocThunk):
        popl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        popl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseArrayAlloc *bmc)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *28(%edx)            # call fivmr_resolveObjectAlloc

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError11
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError11:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the jump
        subl $13, 4(%ebp) # is this right?  dunno yet.

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_resolveInstanceofThunk)

        # Resolve object allocation
FIVMR_SYMBOL(fivmr_resolveInstanceofThunk):
        popl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        popl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl FIVMR_OFFSETOF_REGSAVE_0(%esi)
        
        pushl %ebp               # we use the frame pointer style to help gdb
        movl %esp, %ebp
        andl $-16, %esp           # align the stack

        # save stuff
        pushl %eax
        pushl %ecx
        pushl %edx

        # prepare for call
        pushl %eax
        pushl %eax

        # call the real resolver, which has the signature:
        # void (*)(fivmr_ThreadState *ts, uintptr_t returnAddr, fivmr_BaseInstanceof *bio)
        pushl FIVMR_OFFSETOF_REGSAVE_1(%esi)
        pushl 4(%ebp)
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *32(%edx)            # call fivmr_resolveInstanceof

        # check exception
        cmpl $0, 8(%esi)
        je .LnoError10
        leave
        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)
.LnoError10:

        # recover
        addl $20, %esp
        popl %edx
        popl %ecx
        popl %eax

        # correct the return address so that we're at the jump
        subl $13, 4(%ebp) # is this right?  dunno yet.

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_baselineThrowThunk)

        # helps the baseline-generated code throw an exception.  allows the
        # stack to be in any state it wants to be in.  NOTE: this should
        # never be called.  instead it should be jumped to.
FIVMR_SYMBOL(fivmr_baselineThrowThunk):
        andl $-16, %esp           # align the stack
        pushl %eax               # mis-align for odd number of args

        # create room for result of the call we're about to make.  the result
        # will contain the stack offset and then the address to jump to
        pushl %eax
        pushl %eax
        movl %esp, %edx

        # arguments: ThreadState *ts, uintptr_t framePtr, uintptr_t *result
        pushl %edx
        pushl %ebp
        pushl %esi
        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *8(%edx)            # call fivmr_baselineThrow

        # get back to the result
        popl %eax
        popl %eax
        popl %eax

        # pop the stack offset
        popl %ecx

        # pop the address to jump to
        popl %edx

        # check if the address to jump to is zero, which would mean that we
        # should just return
        test %edx, %edx
        jne .LdontReturnJustJump

.LdontJumpJustReturn:
        # ok, we should return.  clear the return value for good measure.
        # FIXME: what about float returns?  for now we assume that sensible
        # code will assume that the return value is invalid until the exception
        # check is performed.  (this is the case for all compiler-generated
        # code and certainly everything in our runtime.)
        xorl %eax, %eax
        xorl %edx, %edx

        # NOTE: frame popping happens in the C code
        
        # restore esi (it's the only callee-save that baseline uses)
        movl -4(%ebp), %esi

        # return
        leave
        ret

        # this is the case where there is actually an exception handler,
        # so jump to it
.LdontReturnJustJump:
        # adjust the stack
        movl %ebp, %esp
        addl %ecx, %esp

        # push the exception
        pushl 8(%esi)

        # clear the exception
        movl $0, 8(%esi)

        # jump to the address
        jmp *%edx


.globl FIVMR_SYMBOL(fivmr_baselineProEpThrowThunk)

        # helps the baseline-generated code throw an exception from code
        # in the prologue or epilogue, where we don't want try/catch
        # blocks to be taken into account.
FIVMR_SYMBOL(fivmr_baselineProEpThrowThunk):
        movl 16(%esi), %edx       # load curF
        movl 4(%edx), %edx        # load up
        movl %edx, 16(%esi)       # store up into curF
        
        xorl %eax, %eax
        xorl %edx, %edx

        # restore esi (it's the only callee-save that baseline uses)
        movl -4(%ebp), %esi

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_pollcheckSlowBaseline)

        # helps the baseline-generated code perform a pollcheck
FIVMR_SYMBOL(fivmr_pollcheckSlowBaseline):
        pushl %ebp
        movl %esp, %ebp

        andl $-16, %esp  # align the stack

        pushl %eax
        pushl %eax
        
        pushl $-1
        pushl %esi

        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *12(%edx)            # call fivmr_ThreadState_pollcheckSlow

        leave
        ret


.globl FIVMR_SYMBOL(fivmr_nullCheckSlowBaseline)

        # helps the baseline-generated code perform a null check
FIVMR_SYMBOL(fivmr_nullCheckSlowBaseline):
        andl $-16, %esp  # align the stack

        subl $12, %esp

        pushl %esi

        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *16(%edx)            # call fivmr_throwNullPointerRTE_inJava

        addl $16, %esp

        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)


.globl FIVMR_SYMBOL(fivmr_abcSlowBaseline)

        # helps the baseline-generated code perform an array bounds check
FIVMR_SYMBOL(fivmr_abcSlowBaseline):
        andl $-16, %esp  # align the stack

        subl $12, %esp

        pushl %esi

        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *20(%edx)            # call fivmr_throwArrayBoundsRTE_inJava

        addl $16, %esp

        jmp FIVMR_SYMBOL(fivmr_baselineThrowThunk)


.globl FIVMR_SYMBOL(fivmr_stackHeightSlowBaseline)

        # helps the baseline-generated code perform an array bounds check
FIVMR_SYMBOL(fivmr_stackHeightSlowBaseline):
        movl %esp, %eax  # save the stack
        andl $-16, %esp  # align the stack

        pushl %eax
        
        subl $8, %esp

        pushl %esi

        movl 12(%esi), %edx       # load fivmr_VM* into edx
        call *36(%edx)            # call fivmr_throwStackOverflowRTE_inJava

        cmpl $0, 8(%esi)
        jne FIVMR_SYMBOL(fivmr_baselineProEpThrowThunk)

        # I don't think this code will ever get exercised...
        
        addl $12, %esp

        popl %eax
        movl %eax, %esp
        
        ret


