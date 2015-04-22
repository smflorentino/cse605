package test1;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import common.LOG;

/**
 * Created by scottflo on 4/14/15.
 */
public class FibMain {

    public static final int ARRAY_SIZE = 1024;
    public static final int ARRAY_ELEMENT_SIZE = 8;
    public static final int SCOPE_SIZE = 1024 * 4;

    public static final int totalBacking = SCOPE_SIZE + 1024;
    public static void main(String[] args)
    {
        LOG.info("Starting main");

        MemoryAreas.allocScopeBacking(Magic.curThreadState(), totalBacking);

        Pointer scoped = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 3072);

        MemoryAreas.enter(scoped, new Fib2());
    }

}
