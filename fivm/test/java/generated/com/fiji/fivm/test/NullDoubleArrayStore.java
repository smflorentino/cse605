package com.fiji.fivm.test;
public class NullDoubleArrayStore {
   public static void main(String[] v) {
      double[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
