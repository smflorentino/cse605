package com.fiji.fivm.test;
public class NullByteArrayStore {
   public static void main(String[] v) {
      byte[] array=null;
      array[Integer.parseInt(v[0])] = (byte)0;
      System.out.println("got to here.");
   }
}
