package com.fiji.fivm.test;
public class NullObjectArrayStore {
   public static void main(String[] v) {
      Object[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
