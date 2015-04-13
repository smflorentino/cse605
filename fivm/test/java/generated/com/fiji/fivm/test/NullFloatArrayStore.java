package com.fiji.fivm.test;
public class NullFloatArrayStore {
   public static void main(String[] v) {
      float[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
