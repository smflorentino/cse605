/*
 * GraphColoring.java
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

public class GraphColoring {
    
    private GraphColoring() {}
    
    /** Colors the nodes in the given collection.  Returns the maximum color
	used.  Colors start at 0.  Uses the Welsh and Powell algorithm. */
    public static int color(Collection< ? extends Node > nodes) {
        int result;
        long before=System.currentTimeMillis();
	if (nodes.isEmpty()) {
            result=-1;
	} else {
            Node[] sortedNodes=new Node[nodes.size()];
            nodes.toArray(sortedNodes);
            Arrays.sort(
                sortedNodes,
                new Comparator< Node >(){
                    public int compare(Node a,Node b) {
                        if (a.degree()<b.degree()) {
                            return 1;
                        } else if (a.degree()==b.degree()) {
                            return 0;
                        } else {
                            return -1;
                        }
                    }
                });
            Node[][] edges=new Node[sortedNodes.length][];
            for (int i=0;i<sortedNodes.length;++i) {
                Node[] curEdges=new Node[sortedNodes[i].edges.size()];
                sortedNodes[i].edges.toArray(curEdges);
                edges[i]=curEdges;
            }
            int maxColor=-1;
            int totalColorsAssigned=0;
            for (int curColor=0;;++curColor) {
                assert curColor<sortedNodes.length
                    : "curColor = "+curColor+", sortedNodes = "+Util.dump(sortedNodes);
                int colorsAssigned=0;
                for (int i=0;i<sortedNodes.length;++i) {
                    Node n=sortedNodes[i];
                    if (!n.hasColor()) {
                        Node[] e=edges[i];
                        boolean found=false;
                        for (int j=0;j<e.length;++j) {
                            if (e[j].color()==curColor) {
                                found=true;
                                break;
                            }
                        }
                        if (!found) {
                            colorsAssigned++;
                            n.color=curColor;
                            maxColor=curColor;
                        }
                    }
                }
                if (Global.verbosity>=3) {
                    Global.log.println(
                        "Assigned "+curColor+" to "+colorsAssigned+" nodes.");
                }
                if (colorsAssigned==0) {
                    break;
                }
                totalColorsAssigned+=colorsAssigned;
                if (totalColorsAssigned==sortedNodes.length) {
                    break;
                }
            }
            result=maxColor;
        }
        if (Global.verbosity>=2) {
            long after=System.currentTimeMillis();
            Global.log.println("graph coloring for "+nodes.size()+" nodes took "+(after-before)+" ms");
        }
        return result;
    }
    
    @SuppressWarnings("unchecked") // eclipse doesn't want it but javac does.
    public static void cluster(Collection< ? extends Node > nodes_) {
	ArrayList< ? extends Node > nodes;
	if (nodes_ instanceof ArrayList) {
	    nodes=(ArrayList< ? extends Node >)nodes_;
	} else {
	    ArrayList< Node > myNodes=new ArrayList< Node >();
	    myNodes.addAll(nodes_);
	    nodes=myNodes;
	}
	for (int i=0;i<nodes.size();++i) {
	    for (int j=i+1;j<nodes.size();++j) {
		nodes.get(i).addEdge(nodes.get(j));
	    }
	}
    }
    
    public static class Node {
	HashSet< Node > edges;
	int degree;
	int color;
	
	public Node() {
	    edges=new HashSet< Node >();
	    degree=0;
	    color=-1;
	}
	
	public void addEdge(Node other) {
            if (this!=other) {
                if (other.edges.add(this)) {
                    edges.add(other);
                    degree++;
                    other.degree++;
                }
            }
	}
	
	public boolean hasEdge(Node other) {
	    return edges.contains(other);
	}
	
	public int degree() { return degree; }
	
	public int color() { return color; }
	public boolean hasColor() { return color>=0; }

        public void forceColor(int color) {
            this.color=color;
        }
	
        public String description() {
            return null;
        }
        
        public final String toStringLight() {
	    return "GraphColoring.Node[degree = "+degree+
		", color = "+color+", description = "+description()+"]";
        }
        
	public final String toString() {
            StringBuilder buf=new StringBuilder();
            boolean first=true;
            for (Node n : edges) {
                if (first) {
                    first=false;
                } else {
                    buf.append(", ");
                }
                buf.append(n.toStringLight());
            }
	    return "GraphColoring.Node[edges = ["+buf.toString()+"], degree = "+degree+
		", color = "+color+", description = "+description()+"]";
	}
    }
}

