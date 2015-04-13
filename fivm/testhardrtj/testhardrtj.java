import com.fiji.hardrtj.*;

public class testhardrtj {
    static Integer[] array;
    
    static double cyclesPerMS;

    static class Stats {
        int iterations;
        long maxDuration;
        long[] durations=new long[1000];
        int durationI;
        
        void add(long duration) {
            iterations++;
            if (duration>maxDuration) {
                maxDuration=duration;
            }
            durations[durationI++]=duration;
            if (durationI==durations.length) {
                durationI=0;
            }
        }
        
        double max() {
            return maxDuration/cyclesPerMS;
        }
        
        double avg() {
            long sum=0;
            for (int i=0;
                 i<Math.min(iterations,durations.length);
                 ++i) {
                sum+=durations[i];
            }
            return sum
                / ((double)Math.min(iterations,durations.length))
                / cyclesPerMS;
        }

        void report() {
            System.out.println("   Number of iterations: "+iterations);
            System.out.println("   Max duration:         "+max()+" ms");
            System.out.println("   Average duration:     "+avg()+" ms");
        }
    }
    
    static Stats withoutGC = new Stats();
    static Stats withGC = new Stats();
    
    public static void main(String[] v) throws Throwable {
        System.out.println("Figuring out processor speed...");
        long beforeCPU=HardRT.readCPUTimestamp();
        long beforeOS=System.currentTimeMillis();
        Thread.sleep(1000);
        long afterCPU=HardRT.readCPUTimestamp();
        long afterOS=System.currentTimeMillis();
        System.out.println("Slept for "+(afterOS-beforeOS)+
                           " ms according to OS.");
        System.out.println("Slept for "+(afterCPU-beforeCPU)+
                           " cycles according to CPU");
        cyclesPerMS=((double)(afterCPU-beforeCPU))/(afterOS-beforeOS);
        System.out.println("Cycles per millisecond: "+cyclesPerMS);
        
        final Timer t=new Timer();
        t.fireAfter(10,new Runnable(){
                public void run() {
                    long before=HardRT.readCPUTimestamp();
                    if (array==null) {
                        array=new Integer[2000];
                        for (int i=0;i<array.length;++i) {
                            array[i]=new Integer(i);
                        }
                    } else {
                        for (int i=0;i<array.length;++i) {
                            if (!array[i].equals(new Integer(i))) {
                                throw new Error(
                                    "verification failed at i = "+i+".");
                            }
                        }
                        array=null;
                    }
                    t.fireAfter(10,this);
                    long after=HardRT.readCPUTimestamp();
                    if (GC.inProgress()) {
                        withGC.add(after-before);
                    } else {
                        withoutGC.add(after-before);
                    }
                }
            });
        
        for (;;) {
            Thread.sleep(5000);
            System.out.println();
            System.out.println();
            System.out.println("ITERATIONS // NO GC:");
            withoutGC.report();
            System.out.println("ITERATIONS // WITH GC:");
            withGC.report();
            System.out.println("Total iterations:    "+
                               (withGC.iterations+withoutGC.iterations));
            System.out.println("Average WithGC/NoGC: "+
                               (withGC.avg()/withoutGC.avg()));
            System.out.println("Average WithGC-NoGC: "+
                               (withGC.avg()-withoutGC.avg())+" ms");
        }
    }
}

