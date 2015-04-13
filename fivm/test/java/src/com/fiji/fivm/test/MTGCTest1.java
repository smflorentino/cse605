/*
 * MTGCTest1.java
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

package com.fiji.fivm.test;

import com.fiji.util.*;
import java.util.*;

public class MTGCTest1 {
    static int numThreads=50;
    static int numMaps=1000;
    static int restartProbability=10000;
    static int topLevelResetProbability=2000;
    static int boxResetProbability=2000;
    static int maxSize=5000;
    static int deleteProbability=4000;
    
    static class MapBox {
        Map< Object, Object > map;
        
        MapBox(Map< Object, Object > map) {
            this.map=map;
        }
    }
    
    static MapBox[] maps;
    static int[] ops;
    static int threadsStarted;

    // NB: my use of '%' is probably wrong - should be ensuring that the numbers
    // are positive before doing that...  but in the interest of keeping the test
    // constant (since it's so darn good at catching bugs), I'll just ignore that
    // problem.
    
    static Map< Object, Object > makeMap(MarsenneTwister mt) {
        if ((mt.nextInt()%2)==0) {
            return new TreeMap< Object, Object >();
        } else {
            return new HashMap< Object, Object >();
        }
    }

    static class Mutator implements Runnable {
        MarsenneTwister mt=new MarsenneTwister();
        Map< Object, Object > makeMap() {
            return MTGCTest1.makeMap(mt);
        }
        Object mutateMap(Map< Object, Object > map,
                         Object stuff) {
            String key=""+mt.nextInt();
            Util.ensure(map.size()<=maxSize);
            if (map.size()==maxSize ||
                (mt.nextInt()%deleteProbability)==0) {
                return map.remove(key);
            } else {
                map.put(key,stuff);
                return null;
            }
        }
        public void run() {
            try {
                Map< Object, Object > subMap=makeMap();
                for (;;) {
                    if ((mt.nextInt()%restartProbability)==0) {
                        synchronized (MTGCTest1.class) {
                            threadsStarted++;
                        }
                        new Thread(new Mutator()).start();
                        return;
                    } else {
                        int i=Math.abs(mt.nextInt()%maps.length);
                        ops[i]++; // unsynchronized but thats ok
                        if ((mt.nextInt()%topLevelResetProbability)==0) {
                            maps[i]=new MapBox(makeMap());
                        } else {
                            MapBox mb=maps[i];
                            synchronized (mb) {
                                if ((mt.nextInt()%boxResetProbability)==0) {
                                    mb.map=makeMap();
                                } else {
                                    subMap=(Map< Object, Object >)mutateMap(mb.map,subMap);
                                }
                            }
                        }
                    
                        if (subMap!=null) {
                            mutateMap(subMap,""+mt.nextInt());
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    public static void main(String[] v) throws Exception {
        long time=Integer.parseInt(v[0])*1000;
        
        MarsenneTwister mt=new MarsenneTwister();
        maps=new MapBox[numMaps];
        ops=new int[numMaps];
        for (int i=0;i<numMaps;++i) {
            maps[i]=new MapBox(makeMap(mt));
        }
        new Thread() {
            public void run() {
                try {
                    for (;;) {
                        SimpleStats mapSize=new SimpleStats();
                        SimpleStats numOps=new SimpleStats();
                        for (int i=0;i<maps.length;++i) {
                            MapBox mb=maps[i];
                            synchronized (mb) {
                                mapSize.add(mb.map.size());
                            }
                            numOps.add(ops[i]);
                        }
                        System.out.println("Stats:");
                        System.out.println("   Map Sizes: "+mapSize);
                        System.out.println("   Num Ops:   "+numOps);
                        System.out.println("   Threads:   "+threadsStarted);
                        Thread.sleep(500);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }.start();
        for (int i=0;i<numThreads;++i) {
            new Thread(new Mutator()).start();
        }
        Thread.sleep(time);
        System.out.println("We didn't crash.  Success.");
        System.exit(0);
    }
}


