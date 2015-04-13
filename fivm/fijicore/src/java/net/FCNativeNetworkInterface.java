package java.net;


import java.util.HashSet;
import java.util.Set;


class FCNativeNetworkInterface
{
  String name;
  Set<InetAddress> addresses;

  private FCNativeNetworkInterface(String name)
  {
    this.name = name;
    addresses = new HashSet<InetAddress>();
  }
 
  public static native FCNativeNetworkInterface[] getVMInterfaces();
  
}