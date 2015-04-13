package org.ovmj.java;

import java.lang.SCJavaVirtualMachine;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

/**
 * The entry point for executing an SCJ Java (based on RTSJ) supporting
 * user-domain
 * 
 * <p>
 * The {@link#main} method of this class is the execution entry point for
 * user-domain code.
 * 
 * @author David Holmes
 */
public final class RealtimeLauncher {

    // FIXME: Why the tests were ever run from here ? UD non-RT tests
    // are only run directly by selecting appropriate "main" method.
    // When run from here, the tests were always linked to the binary.
    // There was no way to compile a binary that would not include the
    // tests.

    public static final boolean supportTests = false;

    /** No instantiation */
    private RealtimeLauncher() {
    }

    /**
     * The entry point for execution of the user-domain code. This is invoked by
     * the OVM.
     * <p>
     * <b>Important:</b> Note that class initialization is not enabled when we
     * invoke this method. This means that we can not rely on any static
     * initialization until after we have enabled class initialization - which
     * is done either here or in the JVM depending on the config. It also means
     * that we cannot reference any String literal before class initialization
     * is enabled otherwise we will crash due to nested NPE's. It also means
     * that it's probably impossible to actually throw any of the exceptions we
     * try to catch (before class initialization) as the creation of the
     * exception will trigger a nested NPE.
     * 
     * <p>
     * <b>Note:</b> we can't pass in args from the ED as we can't create UD
     * strings until after class initialization is enabled and we can deal with
     * encoding. We keep the typical "main" signature for convenience.
     * 
     */
    static void main(String[] notUsed) {
        Utils.debugPrintln("[SCJ] RealtimeLauncher.main() started");
       
        Utils.debugPrintln("[SCJ] Setting allocation context to immportal");
        VMSupport.setCurrentArea(VMSupport.getImmortalArea());        
        Utils.debugPrintln("[SCJ] immportal... ok.");
        
        
        boolean runTests = false;

        // no exceptions should escape
        try {

            // arguments are obtained by call-backs
            java.lang.SCJavaVirtualMachine.init();

            // now we can handle Strings
            String[] args = LibraryImports.getCommandlineArgumentStringArray();

            // our args start with - and must come before the application
            // class name
            for (int i = 0; i < args.length; i++) {
                if (args[i].charAt(0) != '-')
                    break;
                if (args[i].equals("-dotests")) {
                    runTests = true;
                    if (!supportTests) {
                        throw new RuntimeException(
                                "Tests are now disabled in code.\n");
                    }
                    break;
                }
            }

            if (supportTests) {
                // first run the tests "outside" the JVM
                try {
                    if (runTests) {
                        // Fix me: we can't parse strings yet so we can't let
                        // any test programs think they have args to parse
                        test.TestSuite.main(new String[0]);
                    }
                } catch (Throwable t) {
                    System.err.println("RTJVM Config - TestSuite failed: " + t);
                    t.printStackTrace();
                }

            }            
            // launch the RT-JVM
            java.lang.SCJavaVirtualMachine.getInstance().run();
            
        } catch (Throwable t) {
            LibraryImports.enableClassInitialization(); // may be necessary -HY

            // If this fails due to exceptions in the String handling then
            // we have a problem. Of course if the problem is in the
            // exception handling then we'll probably never get here anyway.
            // If we're really paranoid we should avoid string concat too.

            // this is deliberately two calls in-case t.toString fails.
            // Exceptions will propagate and cause a panic - which is fine
            // as the user-domain has crashed.
            LibraryImports
                    .printString("RT-LAUNCHER: Uncaught user-domain exception: ");
            LibraryImports.printString(t + "\n");
            // try and do stack trace the simple way
            try {
                t.printStackTrace(System.out);
            } catch (Throwable t1) {
                LibraryImports
                        .printString("\nFailure printing stack trace - trying again\n");
                try {
                    StackTraceElement[] st = t.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            LibraryImports.printString(st[i] + "\n");
                        }
                    }
                } catch (Throwable t2) {
                    LibraryImports
                            .printString("-- Failure printing stack trace\n");
                }
            }
        } finally {
        	// here we call the shutdown sequence of OVM
        	java.lang.SCJavaVirtualMachine.getSCJInstance().shutdownSCJ();
        }
    }
}
