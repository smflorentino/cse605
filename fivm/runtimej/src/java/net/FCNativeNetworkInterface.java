/*
 * FCNativeNetworkInterface.java
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


package java.net;

import java.util.HashSet;
import java.util.Vector;
import java.util.Set;
import com.fiji.fivm.r1.*;

public class FCNativeNetworkInterface
{
  String name;
  Set<InetAddress> addresses;

  private FCNativeNetworkInterface(String name)
  {
    this.name = name;
    addresses = new HashSet<InetAddress>();
  }
 
  public static FCNativeNetworkInterface[] getVMInterfaces()
      throws SocketException{
      Pointer res = libc.getVMInterfaces();
      //loop and find lenght
      int length = 0;
      Pointer itr = res;
      // FIXME: use try-finally
      try{
	  while(itr != Pointer.zero()){
	      itr = CType.getPointer(itr, "struct ifaddrs", "ifa_next");
	      length++;
	  }
	  Vector <FCNativeNetworkInterface> nNIs = new Vector<FCNativeNetworkInterface>();
	  //fill in array
	  itr = res;
	  int i=0;
	  while(itr != Pointer.zero()){
	      String newName = fivmRuntime.fromCStringFull(
							   CType.getPointer(itr,"struct ifaddrs","ifa_name"));
	      boolean found = false;
	      for(int j=0 ; j < nNIs.size(); j++){
		  if(nNIs.get(j).name.equals(newName)){
		      nNIs.get(j).addAddress(itr);
		      found = true;
		      break;
		  }
	      }
	      if(!found){
		  FCNativeNetworkInterface nNI = new FCNativeNetworkInterface(
			 fivmRuntime.fromCStringFull(

			     CType.getPointer(itr,"struct ifaddrs","ifa_name")));
		  nNI.addAddress(itr);
		  nNIs.addElement(nNI);
	      }
	      itr = CType.getPointer(itr, "struct ifaddrs", "ifa_next");
	      i++;

	  }
	  return nNIs.toArray(new FCNativeNetworkInterface[nNIs.size()]);
      }
      catch(SocketException e){  
	  throw e;
      }
      finally{
	  libc.freeifaddrs(res);
      }
  }
  
    private void addAddress(Pointer ifaddr) throws SocketException{
	byte[] host;
	Pointer hostPtr;
	Pointer sockaddr = CType.getPointer(ifaddr, "struct ifaddrs", "ifa_addr");
	if(sockaddr == Pointer.zero()){
	    return; //null is valid here
	}
	try{
	    int family=CType.getByte(sockaddr,"struct sockaddr","sa_family");
	    if (family==CVar.getByte("AF_INET6")) {
		host=new byte[16];
		hostPtr=sockaddr.add(CType.offsetof("struct sockaddr_in6","sin6_addr"));
		for (int i=0;i<host.length;++i) {
		    host[i]=hostPtr.add(i).loadByte();
		}
		addresses.add(InetAddress.getByAddress(host));
	    } else if (family==CVar.getByte("AF_INET")) {
		host=new byte[4];
		hostPtr=sockaddr.add(CType.offsetof("struct sockaddr_in","sin_addr"));
		for (int i=0;i<host.length;++i) {
		    host[i]=hostPtr.add(i).loadByte();
		}
		addresses.add(InetAddress.getByAddress(host));
	    }
            // else {
	    //throw new SocketException("Bad sa_family: "+family);
	    //}
	    
	} catch(UnknownHostException e){
	    throw new SocketException("UnknowHost");
	}
    }
  
}
