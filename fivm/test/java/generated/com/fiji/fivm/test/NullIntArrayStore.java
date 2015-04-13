package com.fiji.fivm.test;
public class NullIntArrayStore {
   public static void main(String[] v) {
      int[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
