package com.fiji.fivm.test;
public class NullLongArrayStore {
   public static void main(String[] v) {
      long[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
