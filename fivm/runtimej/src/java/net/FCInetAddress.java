/*
 * FCInetAddress.java
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

import com.fiji.fivm.r1.*;

class FCInetAddress
{
    private FCInetAddress() {}
    
    public static String getLocalHostname(){	
	return libc.getHostName();
    }

    public static byte[] lookupInaddrAny(){
	return new byte[4];
    }

  
  public static String getHostByAddr(byte[] ip)
      throws UnknownHostException{
      String res;
      if(ip.length == 4){
	  res = libc.gethostbyaddr(ip, 4);
      }
      else  if(ip.length ==16){
	  res = libc.gethostbyaddr(ip, 16);
      }
      else throw new UnknownHostException();
      return res;
  }

 
  public static byte[][] getHostByName(String hostname)
      throws UnknownHostException{
      return libc.gethostbyname(hostname);
  }

 
  public static byte[] aton(String address){
      return libc.inet_aton(address);
  }
}
