/*
 * SimpleStats.java
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

/**
 * A very simple and light-weight implementation of descriptive statistics.  This class
 * will measure the minimum, maximum, sum, mean, variance, and standard deviation of
 * a stream of values.  All computations are done using double precision floating
 * point.  Each instance requires a constant amount of space and each operation requires
 * a constant amount of time.  Note however, that the use of constant space means that
 * the precision will degrade as the number of counts gets large.  Do not use this
 * class if you have millions of samples.
 */
public class SimpleStats extends Addable {
    double min,max,sum,sum_2,n;
    
    /**
     * Create an instance initialized with zero samples.
     */
    public SimpleStats() {}
    
    /**
     * Add a sample.  This recomputes the minimum, maximum, count, sum, and sum squared,
     * which can then be used to compute the mean, variance, and standard deviation.
     */
    public void add(double value) {
        if (n==0.0) {
            min=value;
            max=value;
        } else {
            min=Math.min(value,min);
            max=Math.max(value,max);
        }
        sum+=value;
        sum_2+=value*value;
        n++;
    }
    
    public void add(long value) {
        add((double)value);
    }
    
    /**
     * Get the minimum sample value.  If there are no samples, returns zero.
     * @return the minimum sample value.
     */
    public double getMin() {
        return min;
    }
    
    /**
     * Get the maximum sample value.  If there are no samples, returns zero.
     * @return the maximum sample value.
     */
    public double getMax() {
        return max;
    }
    
    /**
     * Get the sum of all sample values.
     * @return the sum of all sample values.
     */
    public double getSum() {
        return sum;
    }
    
    /**
     * Get the sum of all sample values squared.  I.e. given a sequence of
     * calls to the add() method: add(a), add(b), add(c), ..., add(z), this
     * will return a^2 + b^2 + c^2 + ... + z^2.
     * @return the sum of all sample values squared.
     */
    public double getSumSquared() {
        return sum_2;
    }
    
    /**
     * Get the number of samples.  This corresponds to the number of calls
     * to the add() method.
     */
    public double getCount() {
        return n;
    }
    
    /**
     * Get the average of all sample values.  If there are no samples, returns
     * zero.
     * @return the average of all sample values.
     */
    public double getAverage() {
        if (n==0.0) {
            return 0.0;
        } else {
            return sum/n;
        }
    }
    
    /**
     * Get the variance of all sample values.  If there are no samples,
     * returns zero.
     * @return the variance of all sample values.
     */
    public double getVariance() {
        if (n==0.0) {
            return 0.0;
        } else {
            double avg=getAverage();
            return sum_2/n-avg*avg;
        }
    }
    
    /**
     * Get the standard deviation (with bias) of all sample values.  If there are
     * no samples, returns zero.
     * @return the standard deviation of all sample values.
     */
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }
    
    /**
     * Produces a one-line string that describes the minimum, maximum, average, and
     * standard deviation.
     * @return a description of the minimum, maximum, average, and standard deviation.
     */
    public String toString() {
        return "[Min:"+getMin()+" Max:"+getMax()+" Sum:"+getSum()+" Avg:"+getAverage()+" StdDev:"+getStdDev()+"]";
    }
}

