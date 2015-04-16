package test1;

import com.fiji.fivm.Settings;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoScopeChecks;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.test.Fail;

import javax.realtime.IllegalAssignmentError;
import javax.realtime.ThrowBoundaryError;

public class FibTest {
    private static final int scopeSize = 40960;
    private static final int totalBacking = scopeSize * 4 + 1024;

    private static class StaticRunnable implements Runnable {
        static StringBuffer sb;

        static {
            sb = new StringBuffer();
        }

        public void run() {
            System.out.println("Running static runnable");
            if (MemoryAreas.areaOf(sb) != MemoryAreas.getImmortalArea()) {
                throw new Fail("Static Object not allocated in immortal area");
            }
        }
    }

    private static class DepthCheck implements Runnable {
        final Pointer parent;
        final Pointer mine;
        final int depth;
        final int total;

        public DepthCheck(int depth, Pointer parent, Pointer mine) {
            this(depth, depth, parent, mine);
        }

        public DepthCheck(int total, int depth, Pointer parent,
                          Pointer mine) {
            this.parent = parent;
            this.mine = mine;
            this.depth = depth;
            this.total = total;
        }

        public void run() {
            StringBuffer sb = new StringBuffer();
            Pointer current = MemoryAreas.areaOf(sb);
            if (current == parent) {
                throw new Fail("child scope backing store ("
                        + Long.toHexString(current.asLong())
                        + ") matches parent ("
                        + Long.toHexString(parent.asLong())
                        + ") at depth " + depth);
            }
            if (current != mine) {
                throw new Fail("backing store ("
                        + Long.toHexString(current.asLong())
                        + ") does not match declared ("
                        + Long.toHexString(mine.asLong())
                        + ") at depth " + depth);
            }
            if (depth == 0)
                return;
            Pointer area = MemoryAreas.alloc(scopeSize, false, null);
            if (area == mine) {
                throw new Fail("newly created backing store ("
                        + Long.toHexString(area.asLong())
                        + ") matches current ("
                        + Long.toHexString(mine.asLong())
                        + ") at depth " + depth);
            }
            DepthCheck next = new DepthCheck(total, depth - 1, mine, area);
            MemoryAreas.enter(area, next);
            MemoryAreas.pop(area);
            MemoryAreas.free(area);
        }
    }

    private static class PredictableSize {
        public int i;
        public double d;
        public boolean b;

        PredictableSize(int i, double d, boolean b) {
            this.i = i;
            this.d = d;
            this.b = b;
        }
    }

    public static void main(String[] args) {
        System.out.println("Checking raw scope interface");

        final Pointer outer = MemoryAreas.getCurrentArea();
        if (outer == MemoryAreas.getImmortalArea()) {
            throw new Fail("Heap execution in immortal area");
        }

        MemoryAreas.allocScopeBacking(Magic.curThreadState(), totalBacking);

        StringBuffer sb = new StringBuffer();
        if (MemoryAreas.areaOf(sb) != outer) {
            throw new Fail("Object created in unpredictable area");
        }

        final Pointer area1 = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(area1, new Runnable() {
            public void run() {
                Pointer inner = MemoryAreas.getCurrentArea();
                if (inner != area1) {
                    throw new Fail("Inner area does not equal declared area");
                }
                if (MemoryAreas.setCurrentArea(outer) != inner) {
                    throw new Fail("setCurrentArea returned incorrect fivmr_MemoryArea");
                }
                StringBuffer sb = new StringBuffer();
                if (MemoryAreas.areaOf(sb) != outer
                        || MemoryAreas.getCurrentArea() != outer) {
                    throw new Fail("setCurrentArea failed");
                }
                System.out.println("MemoryAreas.setCurrentArea() seems to work");
            }
        });
        MemoryAreas.pop(area1);
        MemoryAreas.free(area1);

        final Pointer area2 = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(area2, new Runnable() {
            public void run() {
                System.out.println("Running anonymous runnable");
                Pointer inner = MemoryAreas.getCurrentArea();
                if (inner == outer) {
                    throw new Fail("Private memory allocation occurred in global memory");
                }
                if (inner != area2) {
                    throw new Fail("Inner area does not equal declared area");
                }
                StaticRunnable sr = new StaticRunnable();
                if (MemoryAreas.areaOf(sr) != inner) {
                    throw new Fail("Inner allocation not in inner area");
                }
                sr.run();
            }
        });
        MemoryAreas.pop(area2);
        MemoryAreas.free(area2);

        Pointer area = MemoryAreas.alloc(scopeSize, false, null);
        System.out.println("Running nested area check");
        DepthCheck dc = new DepthCheck(3, outer, area);
        MemoryAreas.enter(area, dc);
        MemoryAreas.pop(area);
        MemoryAreas.free(area);

        System.out.println("Trying to throw a legal exception from inside a scope");

        area = MemoryAreas.alloc(scopeSize, false, null);
        try {
            final RuntimeException e = new RuntimeException();
            MemoryAreas.enter(area, new Runnable() {
                public void run() {
                    System.out.println("Throwing RuntimeException");
                    throw e;
                }
            });
        } catch (RuntimeException e) {
            System.out.println("Caught exception");
        } finally {
            MemoryAreas.pop(area);
            MemoryAreas.free(area);
        }

        System.out.println("Trying to throw an illegal exception from inside a scope");
        area = MemoryAreas.alloc(scopeSize, false, null);
        boolean caught = false;
        try {
            MemoryAreas.enter(area, new Runnable() {
                public void run() {
                    System.out.println("Throwing RuntimeException");
                    throw new RuntimeException();
                }
            });
        } catch (ThrowBoundaryError e) {
            caught = true;
            System.out.println("Caught exception as ThrowBoundaryError");
        } finally {
            MemoryAreas.pop(area);
            MemoryAreas.free(area);
        }
        if (!caught) {
            throw new Fail("Did not generate ThrowBoundaryError");
        }

        area = MemoryAreas.alloc(scopeSize, false, null);
        System.out.println("Trying illegal assignment");
        MemoryAreas.enter(area, new Runnable() {
            public void run() {
                Pointer area2 = MemoryAreas.alloc(scopeSize, false, null);
                final Object[] a1 = new Object[2];
                final StringBuffer sb1 = new StringBuffer();
                a1[0] = sb1;
                MemoryAreas.enter(area2, new Runnable() {
                    public void run() {
                        Object[] a2 = new Object[2];
                        StringBuffer sb2 = new StringBuffer();
                        boolean caught = false;
                        a2[0] = sb1;
                        a2[1] = sb2;
                        if (MemoryAreas.areaOf(a1) == MemoryAreas.areaOf(sb2)) {
                            throw new Fail("Objects are in the same scope");
                        }
                        try {
                            a1[1] = sb2;
                        } catch (IllegalAssignmentError e) {
                            caught = true;
                            System.out.println("Caught illegal assignment");
                        }
                        if (!caught) {
                            MemoryAreas.setCurrentArea(outer);
                            throw new Fail("Failed to catch illegal assignment");
                        }
                    }
                });
                MemoryAreas.pop(area2);
                MemoryAreas.free(area2);
            }
        });
        MemoryAreas.pop(area);
        MemoryAreas.free(area);

        System.out.println("Trying illegal assignment with @NoScopeChecks");
        area = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(area, new Runnable() {
            public void run() {
                Pointer area2 = MemoryAreas.alloc(scopeSize, false, null);
                final Object[] a1 = new Object[2];
                final StringBuffer sb1 = new StringBuffer();
                a1[0] = sb1;
                MemoryAreas.enter(area2, new Runnable() {
                    @NoScopeChecks
                    public void run() {
                        Object[] a2 = new Object[2];
                        StringBuffer sb2 = new StringBuffer();
                        boolean caught = false;
                        a2[0] = sb1;
                        a2[1] = sb2;
                        if (MemoryAreas.areaOf(a1) == MemoryAreas.areaOf(sb2)) {
                            throw new Fail("Objects are in the same scope");
                        }
                        try {
                            a1[1] = sb2;
                        } catch (IllegalAssignmentError e) {
                            MemoryAreas.setCurrentArea(outer);
                            throw new Fail("IllegalAssignmentError was raised");				}
                    }
                });
                MemoryAreas.pop(area2);
                MemoryAreas.free(area2);
            }
        });
        MemoryAreas.pop(area);
        MemoryAreas.free(area);

        System.out.println("Trying legal return");
        final Pointer lrarea = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(lrarea, new Runnable() {
            public void run() {
                Pointer area2 = MemoryAreas.alloc(scopeSize, false, null);
                MemoryAreas.enter(area2, new Runnable() {
                    @NoInline
                    public Object allocObject() {
                        return new Object[1];
                    }

                    public void run() {
                        Object a[] = new Object[1];
                        MemoryAreas.setCurrentArea(lrarea);
                        a[0] = allocObject();
                    }
                });
                MemoryAreas.pop(area2);
                MemoryAreas.free(area2);
            }
        });
        MemoryAreas.pop(lrarea);
        MemoryAreas.free(lrarea);

        System.out.println("Trying illegal return");
        final Pointer irarea = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(irarea, new Runnable() {
            @NoInline
            public Object allocObject() {
                return new Object[1];
            }

            public void run() {
                MemoryAreas.setCurrentArea(outer);
                Object[] a = new Object[1];
                MemoryAreas.setCurrentArea(irarea);
                boolean caught = false;
                try {
                    a[0] = allocObject();
                } catch (IllegalAssignmentError e) {
                    caught = true;
                    System.out.println("Caught illegal return");
                }
                if (!caught) {
                    MemoryAreas.setCurrentArea(outer);
                    throw new Fail("Failed to catch illegal return");
                }
            }
        });
        MemoryAreas.pop(irarea);
        MemoryAreas.free(irarea);

        System.out.println("Triggering OOME");
        area = MemoryAreas.alloc(scopeSize, false, null);
        MemoryAreas.enter(area, new Runnable() {
            public void run() {
                boolean caught = false;
                try {
                    Object[] a = new Object[scopeSize];
                } catch (OutOfMemoryError e) {
                    caught = true;
                    System.out.println("Caught OOME");
                }
                if (!caught) {
                    MemoryAreas.setCurrentArea(outer);
                    throw new Fail("OOME was not generated");
                }
            }
        });
        MemoryAreas.pop(area);
        MemoryAreas.free(area);

        System.out.println("Scoped memory interface seems to work");
    }
}
