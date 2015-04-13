#!/usr/bin/env ruby
#
# genNullArrTest.rb
# Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
# This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
# LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
# available at fivm/LEGAL and can also be found at
# http://www.fiji-systems.com/FPL3.txt
# 
# By installing, reproducing, distributing, and/or using the FIJI VM Software
# you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
# rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
# restrictions stated therein.  Among other conditions and restrictions, the
# FIJI PUBLIC LICENSE states that:
# 
# a. You may only make non-commercial use of the FIJI VM Software.
# 
# b. Any adaptation you make must be licensed under the same terms 
# of the FIJI PUBLIC LICENSE.
# 
# c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
# file, adaptation or output code that you distribute and cause the output code
# to provide a notice of the FIJI PUBLIC LICENSE. 
# 
# d. You must not impose any additional conditions.
# 
# e. You must not assert or imply any connection, sponsorship or endorsement by
# the author of the FIJI VM Software
# 
# f. You must take no derogatory action in relation to the FIJI VM Software
# which would be prejudicial to the FIJI VM Software author's honor or
# reputation.
# 
# 
# The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
# representation and provides no warranty of any kind concerning the software.
# 
# The FIJI PUBLIC LICENSE and any rights granted therein terminate
# automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
#
#

$types=['boolean','byte','char','short','int','long','float','double','Object']

def camelCase(str)
  str[0..0].upcase+str[1..-1].downcase
end

# generate the load tests

$types.each {
  | type |
  File.open("test/java/generated/com/fiji/fivm/test/Null#{camelCase(type)}ArrayLoad.java","w") {
    | outp |
    outp.puts "package com.fiji.fivm.test;"
    outp.puts "public class Null#{camelCase(type)}ArrayLoad {"
    outp.puts "   public static void main(String[] v) {"
    outp.puts "      #{type}[] array=null;"
    outp.puts "      System.out.println(array[Integer.parseInt(v[0])]);"
    outp.puts "      System.out.println(\"got to here.\");"
    outp.puts "   }"
    outp.puts "}"
  }
}

# generate the store tests

$types.each {
  | type |
  File.open("test/java/generated/com/fiji/fivm/test/Null#{camelCase(type)}ArrayStore.java","w") {
    | outp |
    outp.puts "package com.fiji.fivm.test;"
    outp.puts "public class Null#{camelCase(type)}ArrayStore {"
    outp.puts "   public static void main(String[] v) {"
    outp.puts "      #{type}[] array=null;"
    if type=="boolean"
      outp.puts "      array[Integer.parseInt(v[0])] = false;"
    else
      outp.puts "      array[Integer.parseInt(v[0])] = (byte)0;"
    end
    outp.puts "      System.out.println(\"got to here.\");"
    outp.puts "   }"
    outp.puts "}"
  }
}

