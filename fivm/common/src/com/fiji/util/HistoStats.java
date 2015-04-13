/*
 * HistoStats.java
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

package com.fiji.util;

public class HistoStats extends Addable {
    long[] histogram;
    long min;
    long step;
    long underflow;
    long overflow;
    long measuredMin;
    long measuredMax;
    
    public HistoStats(long min,long step,long nsteps) {
        if (nsteps>0x7fffffff) {
            throw new OutOfMemoryError();
        }
        histogram=new long[(int)nsteps];
        this.min=min;
        this.step=step;
    }
    
    public void add(long value) {
        long idx=(value-min)/step;
        if (idx<0) {
            underflow++;
            if (underflow==1) {
                measuredMin=value;
            } else {
                measuredMin=Math.min(measuredMin,value);
            }
        } else if (idx>=histogram.length) {
            overflow++;
            if (overflow==1) {
                measuredMax=value;
            } else {
                measuredMax=Math.max(measuredMax,value);
            }
        } else {
            histogram[(int)idx]++;
        }
    }
    
    int indexOf(long value) {
        long idx=(value-min)/step;
        if (idx<0) {
            throw new IllegalArgumentException("Value underflow: "+value);
        }
        if (idx>=histogram.length) {
            throw new IllegalArgumentException("Value overflow: "+value);
        }
        return (int)idx;
    }
    
    public long innerPopulation() {
        long result=0;
        for (long cnt : histogram) {
            result+=cnt;
        }
        return result;
    }
    
    public long population() {
        return innerPopulation()+overflow+underflow;
    }
    
    public long zdfAt(long value) {
        return histogram[indexOf(value)];
    }
    
    public long pdfAt(long value) {
        int idx=indexOf(value);
        long result=0;
        for (int i=0;i<=idx;++i) {
            result+=histogram[i];
        }
        return result;
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("[");
        for (int i=0;i<histogram.length;++i) {
            long cnt=histogram[i];
            if (cnt>0) {
                buf.append(min+i*step);
                buf.append(":");
                buf.append(cnt);
                buf.append(" ");
            }
        }
        if (underflow>0) {
            buf.append("Unf:");
            buf.append(underflow);
            buf.append(" Min:");
            buf.append(measuredMin);
            buf.append(" ");
        }
        if (overflow>0) {
            buf.append("Ovf:");
            buf.append(overflow);
            buf.append(" Max:");
            buf.append(measuredMax);
            buf.append(" ");
        }
        String result=buf.toString();
        return result.substring(0,result.length()-1)+"]";
    }
}


