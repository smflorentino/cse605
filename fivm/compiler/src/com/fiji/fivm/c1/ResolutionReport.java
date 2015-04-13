/*
 * ResolutionReport.java
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

package com.fiji.fivm.c1;

import java.util.*;
import com.fiji.config.*;
import java.io.*;

public final class ResolutionReport {
    // <thing you wanted to resolve> -> list of <where you were resolving from>
    private LinkedHashMap< ResolutionID, LinkedHashSet< ResolutionID > > map=
        new LinkedHashMap< ResolutionID, LinkedHashSet< ResolutionID > >();
    
    public ResolutionReport() {}
    
    public ResolutionReport(ResolutionReport other) {
        addAll(other);
    }
    
    public ResolutionReport(ConfigNode config) {
        for (ConfigNode node : config.asList()) {
            ConfigMapNode map=node.asMap();
            ResolutionID target=new ResolutionID(map.getNode("target"));
            ConfigListNode sources=map.getList("uses");
            for (ConfigNode node2 : sources) {
                addUse(target,new ResolutionID(node2));
            }
        }
    }
    
    // as far as I can tell the only concurrent use of ResolutionReport is addUse();
    // all others are called only when we're 1-threaded.
    public synchronized void addUse(ResolutionID target,ResolutionID use) {
        if (Global.verbosity>=2) {
            Global.log.println("Unresolved use of ["+target+"] from ["+use+"]");
        }
        LinkedHashSet< ResolutionID > set=map.get(target);
        if (set==null) {
            map.put(target,set=new LinkedHashSet< ResolutionID >());
        }
        set.add(use);
        assert targets().contains(target);
        assert uses(target).contains(use);
    }
    
    public void addUses(ResolutionID target,Iterable< ResolutionID > uses) {
        for (ResolutionID use : uses) {
            addUse(target,use);
        }
    }
    
    public void removeUse(ResolutionID target,ResolutionID use) {
        LinkedHashSet< ResolutionID > set=map.get(target);
        if (set!=null) {
            if (set.remove(use)) {
                if (set.isEmpty()) {
                    map.remove(target);
                }
            }
        }
    }
    
    public void removeUses(ResolutionID target,Iterable< ResolutionID > uses) {
        for (ResolutionID use : uses) {
            removeUse(target,use);
        }
    }
    
    public void addAll(ResolutionReport other) {
        for (ResolutionID target : other.targets()) {
            addUses(target,other.uses(target));
        }
    }
    
    public void removeAll(ResolutionReport other) {
        for (ResolutionID target : other.targets()) {
            removeUses(target,other.uses(target));
        }
    }
    
    public Set< ResolutionID > targets() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    public Set< ResolutionID > uses(ResolutionID target) {
        LinkedHashSet< ResolutionID > result=map.get(target);
        if (result==null) {
            return EMPTY_SET;
        } else {
            return Collections.unmodifiableSet(result);
        }
    }
    
    public ConfigNode asConfigNode() {
        ConfigListNode list=new ConfigListNode();
        for (Map.Entry< ResolutionID, LinkedHashSet< ResolutionID > > e
                 : map.entrySet()) {
            ResolutionID target=e.getKey();
            LinkedHashSet< ResolutionID > uses=e.getValue();
            ConfigMapNode map=new ConfigMapNode();
            map.put("target",target.asConfigNode());
            ConfigListNode list2=new ConfigListNode();
            map.put("uses",list2);
            for (ResolutionID vc : uses) {
                list2.append(vc.asConfigNode());
            }
            list.append(map);
        }
        return list;
    }
    
    public void printReport(PrintWriter w) {
        for (Map.Entry< ResolutionID, LinkedHashSet< ResolutionID > > e
                 : map.entrySet()) {
            ResolutionID target=e.getKey();
            if (target.getClassResolutionMode()!=ClassResolutionMode.RESOLUTION_CANCELED) {
                LinkedHashSet< ResolutionID > uses=e.getValue();
                w.println("WARNING: Could not resolve: "+target.forReport());
                w.println("  Used from:");
                for (ResolutionID use : uses) {
                    w.println("    "+use);
                }
            }
        }
    }
    
    private static Set< ResolutionID > EMPTY_SET=
        Collections.unmodifiableSet(new LinkedHashSet< ResolutionID >());
}

