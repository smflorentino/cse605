package com.fiji.fivm.test;
public class NullShortArrayStore {
   public static void main(String[] v) {
      short[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
