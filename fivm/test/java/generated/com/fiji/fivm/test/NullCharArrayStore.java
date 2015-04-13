package com.fiji.fivm.test;
public class NullCharArrayStore {
   public static void main(String[] v) {
      char[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
