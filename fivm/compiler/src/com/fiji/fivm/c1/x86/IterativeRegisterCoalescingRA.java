/*
 * IterativeRegisterCoalescingRA.java
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

package com.fiji.fivm.c1.x86;

import java.util.*;
import java.io.*;

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

public class IterativeRegisterCoalescingRA extends LCodePhase {
    public IterativeRegisterCoalescingRA(LCode lc) { super(lc); }

    static class Node {
        LArg var;
        LArg color;
        double spillCost;
        
        LinkedHashSet< Node > succs=new LinkedHashSet< Node >();
        LinkedHashSet< Node > preds=new LinkedHashSet< Node >();
        
        Node(Liveness g, LArg var) {
            this.var=var;
            this.index=g.size();
            g.put(var, this);
        }
        
        Set<Node> adj() {
            LinkedHashSet<Node> l = new LinkedHashSet<Node>();
            l.addAll(succs);
            l.addAll(preds);
            return l;
        }

        int inDegree () { return preds.size(); }
        int outDegree() { return succs.size(); }
        int degree   () { return inDegree() + outDegree(); }
        
        boolean goesTo   (Node n) { return succs.contains(n); }
        boolean comesFrom(Node n) { return preds.contains(n); }
        boolean adj      (Node n) { return goesTo(n) || comesFrom(n); }
        
        private int index;
        public String toString() { return "Node["+index+", "+var+", "+color+", "+spillCost+"]"; }
        boolean equals(Node n) { return index == n.index; }
    }
    
    static class Move {
        Node src;
        Node dst;
        
        Move(Node src,Node dst) {
            this.src=src;
            this.dst=dst;
        }

        public String toString() {
            return "Move["+src+" -> "+dst+"]";
        }
    }
    
    class Liveness {
        private LinkedHashMap< LArg, Node > nodes=new LinkedHashMap< LArg, Node >();
        
        Node put(LArg var,Node n) {
            return nodes.put(var,n);
        }
        
        Node get(LArg k) { return nodes.get(k); }
        Set< LArg > vars() { return nodes.keySet(); }
        Collection<Node> nodes() { return nodes.values(); }
        int size() { return nodes.size(); }

        void check(Node n) {
            if (false) {
                if (nodes.containsValue(n)) return;
                throw new Error("Liveness.addEdge using nodes from the wrong graph");
            }
        }

        void addEdge(Node from, Node to) {
            if (false) Global.log.println("from = "+from+", to = "+to);
            check(from); check(to);
            if (from.goesTo(to)) return;
            to.preds.add(from);
            if (false) Global.log.println("  before: from.succs = "+from.succs);
            from.succs.add(to);
            if (false) Global.log.println("  after: from.succs = "+from.succs);
        }

        void rmEdge(Node from, Node to) {
            to.preds.remove(from);
            from.succs.remove(to);
        }

        private Node newNode(LArg temp) {
            Node n = get(temp);
            if (n == null)
                n = new Node(this, temp);
            return n;
        }

        LinkedList<Move> moves = new LinkedList<Move>();

        List<Move> moves() {
            return moves;
        }

        Liveness(Kind kind) {
            // Create precolored nodes for registers
            for (Reg r : Reg.all(kind)) {
                Node n = newNode(r);
                n.color = r;
            }
            
            for (Tmp t : code.tmps()) {
                if (t.kind()==kind) {
                    newNode(t);
                }
            }

            LLivenessCalc lc=code.getLiveness();

            for (LHeader h : code.headers()) {
                LLivenessCalc.LocalCalc llc=lc.new LocalCalc(h);
                for (LOp o : h.reverseOperations()) {
                    if (Global.verbosity>=5) {
                        Global.log.println("At "+o+":");
                        Global.log.println("   defs: "+Util.dump(o.defs()));
                        Global.log.println("   live: "+llc.currentlyLive());
                    }
                    
                    LArg src=null;
                    if (h.probability()==HeaderProbability.DEFAULT_PROBABILITY &&
                        o.opcode()==LOpCode.Mov &&
                        o.rhs(0).variable() &&
                        o.rhs(1).variable() &&
                        o.rhs(0).kind()==kind) {
                        src=o.rhs(0);
                        moves.addFirst(new Move(get(o.rhs(0)),get(o.rhs(1))));
                    }
                    
                    for (LArg d : o.defs()) {
                        if (d.kind()==kind) {
                            Node from=get(d);
                            from.spillCost+=h.probableFrequency();
                            for (LArg t : llc.currentlyLive()) {
                                if (t!=src && t.kind()==kind) {
                                    Node to=get(t);
                                    if (Global.verbosity>=5) {
                                        Global.log.println("Potential interference between "+d+" and "+t);
                                    }
                                    if (from!=to && !from.adj(to)) {
                                        if (from.color==null) {
                                            addEdge(from,to);
                                        }
                                        if (to.color==null) {
                                            addEdge(to,from);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    for (LArg s : o.uses()) {
                        if (s.kind()==kind) {
                            Node n=get(s);
                            n.spillCost+=h.probableFrequency();
                        }
                    }
                    
                    llc.update(o);
                }
            }
            
            for (Node n : nodes()) {
                // this is retarded, but it works.  Trust Me (TM).
                n.spillCost=Math.sqrt(n.spillCost);
            }
        }

        void show(PrintWriter out) {
            for (Node n : nodes()) {
                out.print(n.var.toString());
                out.print(": ");
                for (Node s : n.succs) {
                    out.print(s.var.toString());
                    out.print(" ");
                }
                out.println();
            }
            for (Move move : moves) {
                out.print(move.dst.var.toString());
                out.print(" <= ");
                out.println(move.src.var.toString());
            }
            out.flush();
        }
    }
    
    class Color {
        Kind kind;
        
        LinkedList<LArg> colors = new LinkedList<LArg>();
        Liveness ig;
        int K;

        LArg[] spills() {
            LArg[] spills = null;
            int spillCount = spilledNodes.size();
            if (spillCount > 0) {
                spills = new LArg[spilledNodes.size()];
                int i = 0;
                for (Node n : spilledNodes)
                    spills[i++] = n.var;
            }
            return spills;
        }

        // Node worklists, sets and stacks
        LinkedHashSet<Node> precolored, // machine registers, preassigned a color
            simplifyWorklist, // low-degree non-move-related nodes
            freezeWorklist, // low-degree move-related nodes
            coalescedNodes; // registers that have been coalesced
        LinkedList<Node> initial, // temporary registers, not precolored and
                                  // not yet processed
            spillWorklist, // high-degree nodes
            spilledNodes, // nodes marked for spilling during this round
            coloredNodes, // nodes successfully colored
            selectStack; // stack containing temporaries removed from
                         // the graph
        
        {
            precolored = new LinkedHashSet<Node>();
            initial = new LinkedList<Node>();
            simplifyWorklist = new LinkedHashSet<Node>();
            freezeWorklist = new LinkedHashSet<Node>();
            spillWorklist = new LinkedList<Node>();
            spilledNodes = new LinkedList<Node>();
            coalescedNodes = new LinkedHashSet<Node>();
            coloredNodes = new LinkedList<Node>();
            selectStack = new LinkedList<Node>();
        }

        /**
         * Move sets. There are five sets of move instructions, and every move is in
         * exactly one of these sets (after Build through end of Color).
         */

        /**
         * Moves enabled for possible coalescing.
         */
        LinkedList<Move> worklistMoves = new LinkedList<Move>();
        /**
         * Moves that have been coalesced.
         */
        LinkedHashSet<Move> coalescedMoves = new LinkedHashSet<Move>();
        /**
         * Moves whose source and target interfere.
         */
        LinkedHashSet<Move> constrainedMoves = new LinkedHashSet<Move>();
        /**
         * Moves no longer considered for coalescing.
         */
        LinkedHashSet<Move> frozenMoves = new LinkedHashSet<Move>();
        /**
         * Moves not yet ready for coalescing.
         */
        LinkedHashSet<Move> activeMoves = new LinkedHashSet<Move>();

        /**
         * Other data structures.
         */

        /**
         * Current degree of each node.
         */
        LinkedHashMap<Node, Integer> degree = new LinkedHashMap<Node, Integer>();
        /**
         * Moves associated with each node.
         */
        LinkedHashMap<Node, LinkedList<Move>> moveList =
            new LinkedHashMap<Node, LinkedList<Move>>();
        /**
         * When a move (u,v) has been coalesced, and v put in coalescedNodes, then
         * alias(v) = u
         */
        LinkedHashMap<Node, Node> alias = new LinkedHashMap<Node, Node>();

        LinkedList<Move> moveList(Node n) {
            LinkedList<Move> moves = moveList.get(n);
            if (moves == null) {
                moves = new LinkedList<Move>();
                moveList.put(n, moves);
            }
            return moves;
        }

        void build() {
            if (Global.doRegCoalescing)
                for (Move m : ig.moves()) {
                    worklistMoves.add(m);
                    moveList(m.src).add(m);
                    moveList(m.dst).add(m);
                }
        }

        void addEdge(Node u, Node v) {
            if (u != v && !u.adj(v)) {
                if (!precolored.contains(u)) {
                    ig.addEdge(u, v);
                    degree.put(u, new Integer(degree(u) + 1));
                }
                if (!precolored.contains(v)) {
                    ig.addEdge(v, u);
                    degree.put(v, new Integer(degree(v) + 1));
                }
            }
        }

        void makeWorklist() {
            for (Iterator<Node> i = initial.iterator(); i.hasNext();) {
                Node n = i.next();
                i.remove();
                if (degree(n) >= K)
                    spillWorklist.add(n);
                else if (moveRelated(n))
                    freezeWorklist.add(n);
                else
                    simplifyWorklist.add(n);
            }
        }

        LinkedHashSet<Node> adjacent(Node n) {
            LinkedHashSet<Node> adj = new LinkedHashSet<Node>();
            for (Node s : n.succs)
                adj.add(s);
            adj.removeAll(selectStack);
            adj.removeAll(coalescedNodes);
            return adj;
        }

        LinkedHashSet<Move> nodeMoves(Node n) {
            LinkedHashSet<Move> moves = new LinkedHashSet<Move>();
            moves.addAll(activeMoves);
            moves.addAll(worklistMoves);
            moves.retainAll(moveList(n));
            return moves;
        }

        boolean moveRelated(Node n) {
            return !nodeMoves(n).isEmpty();
        }

        void simplify() {
            Iterator<Node> i = simplifyWorklist.iterator();
            Node n = i.next();
            i.remove();
            selectStack.addFirst(n);
            for (Node m : adjacent(n))
                decrementDegree(m);
        }

        int degree(Node n) {
            Integer d = degree.get(n);
            if (d == null)
                return 0;
            return d.intValue();
        }

        void decrementDegree(Node m) {
            int d = degree(m);
            degree.put(m, new Integer(d - 1));
            if (d == K) {
                LinkedHashSet<Node> nodes = adjacent(m);
                nodes.add(m);
                enableMoves(nodes);
                spillWorklist.remove(m);
                if (moveRelated(m))
                    freezeWorklist.add(m);
                else
                    simplifyWorklist.add(m);
            }
        }

        void enableMoves(LinkedHashSet<Node> nodes) {
            for (Node n : nodes)
                for (Move m : nodeMoves(n))
                    if (activeMoves.contains(m)) {
                        setRem(activeMoves, m);
                        setAdd(worklistMoves, m);
                    }
        }

        void coalesce() {
            Move m = worklistMoves.removeFirst();
            Node x = getAlias(m.src);
            Node y = getAlias(m.dst);
            Node u, v;
            boolean precoloredU, precoloredV;
            if (precolored.contains(y)) {
                u = y;
                v = x;
                precoloredU = true;
                precoloredV = precolored.contains(v);
            } else {
                u = x;
                v = y;
                precoloredU = precolored.contains(u);
                precoloredV = precolored.contains(v);
            }
            if (u == v) {
                setAdd(coalescedMoves, m);
                if (!precoloredU)
                    addWorklist(u);
            } else if (precoloredV || u.adj(v)) {
                setAdd(constrainedMoves, m);
                if (!precoloredU)
                    addWorklist(u);
                if (!precoloredV)
                    addWorklist(v);
            } else if (precoloredU && isOK(u, v) || !precoloredU
                       && conservative(u, v)) {
                setAdd(coalescedMoves, m);
                combine(u, v);
                if (!precoloredU)
                    addWorklist(u);
            } else
                setAdd(activeMoves, m);
        }

        void addWorklist(Node u) {
            if (!moveRelated(u) && degree(u) < K) {
                setRem(freezeWorklist, u);
                setAdd(simplifyWorklist, u);
            }
        }

        boolean isOK(Node u, Node v) {
            for (Node t : adjacent(v)) {
                if (!(precolored.contains(t) || degree(t) < K || t.adj(u)))
                    return false;
            }
            return true;
        }

        boolean conservative(Node u, Node v) {
            LinkedHashSet<Node> nodes = adjacent(u);
            nodes.addAll(adjacent(v));
            int k = 0;
            for (Node n : nodes)
                if (precolored.contains(n) || degree(n) >= K)
                    ++k;
            return k < K;
        }

        Node getAlias(Node n) {
            if (coalescedNodes.contains(n))
                return getAlias(alias.get(n));
            return n;
        }

        void combine(Node u, Node v) {
            if (freezeWorklist.contains(v)) {
                setRem(freezeWorklist, v);
                setAdd(coalescedNodes, v);
            } else {
                setRem(spillWorklist, v);
                setAdd(coalescedNodes, v);
            }
            alias.put(v, u);
            moveList(u).addAll(moveList(v));
            LinkedHashSet<Node> nodes = new LinkedHashSet<Node>();
            nodes.add(v);
            enableMoves(nodes);
            for (Node t : adjacent(v)) {
                addEdge(t, u);
                decrementDegree(t);
            }
            if (freezeWorklist.contains(u) && degree(u) >= K) {
                setRem(freezeWorklist, u);
                setAdd(spillWorklist, u);
            }
        }

        void freeze() {
            Iterator<Node> i = freezeWorklist.iterator();
            Node u = i.next();
            i.remove();
            setAdd(simplifyWorklist, u);
            freezeMoves(u);
        }

        void freezeMoves(Node u) {
            for (Move m : nodeMoves(u)) {
                Node v = getAlias(m.src);
                if (v == getAlias(u))
                    v = getAlias(m.dst);
                setRem(activeMoves, m);
                setAdd(frozenMoves, m);
                if (!precolored.contains(v) && nodeMoves(v).isEmpty()
                    && degree(v) < K) {
                    setRem(freezeWorklist, v);
                    setAdd(simplifyWorklist, v);
                }
            }
        }

        void selectSpill() {
            Node m = null;
            for (Node n : spillWorklist) {
                if (m == null) {
                    m = n;
                } else if (n.var.spillable()) {
                    if (!m.var.spillable()) {
                        m = n;
                        
                    } else if (n.spillCost / degree(n) < m.spillCost / degree(m)) {
                        m = n;
                    }
                }
            }
            setRem(spillWorklist, m);
            setAdd(simplifyWorklist, m);
            freezeMoves(m);
        }

        void assignColors() {
            while (!selectStack.isEmpty()) {
                Node n = selectStack.removeFirst();
                LinkedHashSet<LArg> okColors = new LinkedHashSet<LArg>(colors);
                for (Node w : n.succs) {
                    w = getAlias(w);
                    if (w.color != null)
                        okColors.remove(w.color);
                }
                if (okColors.isEmpty()) {
                    if (Global.verbosity>=5) {
                        Global.log.println("Spilling: "+n);
                    }
                    setAdd(spilledNodes, n);
                } else {
                    LArg r=okColors.iterator().next();
                    if (Global.verbosity>=5) {
                        Global.log.println("Assignment: "+n+" -> "+r);
                    }
                    setAdd(coloredNodes, n);
                    n.color = r;
                }
            }
            for (Node n : coalescedNodes)
                n.color = getAlias(n).color;
        }

        private <R> void setRem(java.util.Collection<R> set, R e) {
            if (!set.remove(e))
                error(e);
        }

        private <R> void setAdd(java.util.Collection<R> set, R e) {
            if (!set.add(e))
                error(e);
        }

        private <R> String error(R e) {
            String error = "";
            if (e instanceof Node) {
                Node n = (Node) e;
                error += n.var + "(" + new Integer(degree(n)) + "):";
                if (precolored.contains(n))
                    error += " precolored";
                if (initial.contains(n))
                    error += " initial";
                if (simplifyWorklist.contains(n))
                    error += " simplifyWorklist";
                if (freezeWorklist.contains(n))
                    error += " freezeWorklist";
                if (spillWorklist.contains(n))
                    error += " spillWorklist";
                if (spilledNodes.contains(n))
                    error += " spilledNodes";
                if (coalescedNodes.contains(n))
                    error += " coalescedNodes";
                if (coloredNodes.contains(n))
                    error += " coloredNodes";
                if (selectStack.contains(n))
                    error += " selectStack";
            } else if (e instanceof Move) {
                Move m = (Move) e;
                error += m.dst + "<=" + m.src + ":";
                if (coalescedMoves.contains(m))
                    error += " coalescedMoves";
                if (constrainedMoves.contains(m))
                    error += " constrainedMoves";
                if (frozenMoves.contains(m))
                    error += " frozenMoves";
                if (worklistMoves.contains(m))
                    error += " worklistMoves";
                if (activeMoves.contains(m))
                    error += " activeMoves";
            }
            throw new Error(error);
        }

        public Color(Kind kind,Liveness ig) {
            this.kind=kind;
            this.ig=ig;
            K = 0;
            for (LArg r : Reg.usables(kind)) {
                Node n = ig.get(r);
                precolored.add(n);
                colors.add(r);
                K++;
            }
            for (Node n : ig.nodes())
                if (n.color == null) {
                    initial.add(n);
                    degree.put(n, new Integer(n.outDegree()));
                }

            build();
            makeWorklist();

            do {
                if (!simplifyWorklist.isEmpty()) {
                    // System.err.println("simplify");
                    simplify();
                } else if (!worklistMoves.isEmpty()) {
                    // System.err.println("coalesce");
                    coalesce();
                } else if (!freezeWorklist.isEmpty()) {
                    // System.err.println("freeze");
                    freeze();
                } else if (!spillWorklist.isEmpty()) {
                    // System.err.println("selectSpill");
                    selectSpill();
                }
            } while (!(simplifyWorklist.isEmpty() && worklistMoves.isEmpty()
                       && freezeWorklist.isEmpty() && spillWorklist.isEmpty()));
            assignColors();
        }
    }
    
    void allocForKind(Kind kind) {
        if (Global.verbosity>=3) {
            Global.log.println("RA["+kind+"] for "+code);
        }
        
        Liveness ig;
        Color color;
        
        for (int cnt=0;;++cnt) {
            ig=new Liveness(kind);
            if (Global.verbosity>=5) {
                ig.show(Global.log);
            }
            color=new Color(kind,ig);
            LArg[] spills=color.spills();
            if (spills==null) {
                break;
            }

            if (cnt==0) {
                new SlowPathLiveRangeSplit(code).doit();
                new LSimplifyFixpoint(code).doit();
                continue;
            }

            if (Global.verbosity>=5) {
                Global.log.println("Spills: "+Util.dump(spills));
            }
            
            new Spill(code,spills).doit();
            
            if (cnt>1000 && Global.verbosity>=1) {
                Global.log.println("Register allocation stuck in "+code);
            }
        }
        
        HashMap< LArg, LArg > map=new HashMap< LArg, LArg >();
        for (LArg a : ig.vars()) {
            Reg r=(Reg)ig.get(a).color;
            if (r!=a) {
                if (Global.verbosity>=5) {
                    Global.log.println("Allocation: "+a+" -> "+r);
                }
                
                assert r.isUsable() : "a = "+a+", r = "+r;
                if (r.isPersistent()) {
                    code.usePersistent(r);
                }
                map.put(a,r);
            }
        }
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                o.mapRhs(map);
            }
        }
        
        setChangedCode();
        code.killAllAnalyses();
    }
    
    public void visitCode() {
        allocForKind(Kind.FLOAT);
        allocForKind(Kind.INT);
        code.delAllTmps();
        code.registersAllocated=true;
    }
}

