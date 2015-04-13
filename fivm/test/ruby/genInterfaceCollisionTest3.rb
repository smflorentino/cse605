#!/usr/bin/env ruby
#
# genInterfaceCollisionTest3.rb
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

puts "package com.fiji.fivm.test;"
puts "import com.fiji.fivm.util.CLStats;"
puts "public class InterfaceCollisionTest3 {"

150.times {
  | idx |
  puts "   static interface I#{idx} {"
  puts "      public void foo#{idx}();"
  puts "   }"
}

2.times {
  | idx |
  ifaces=[]
  100.times {
    | jdx |
    ifaces << "I#{idx*50+jdx}"
  }
  puts "   static class C#{idx} implements #{ifaces.join(', ')} {"
  100.times {
    | jdx |
    puts "      public void foo#{idx*50+jdx}() {"
    puts "         System.out.println(\"C#{idx}.foo#{idx*50+jdx} called\");"
    puts "      }"
  }
  puts "   }"
}

puts "   public static void main(String[] v) {"

2.times {
  | idx |
  puts "      Object o#{idx} = new C#{idx}();"
  puts "      System.out.println(\"Allocation #{idx}: \"+o#{idx});"
}

2.times {
  | idx |
  puts "      Util.ensureEqual(o#{idx}.getClass(),C#{idx}.class);"
  2.times {
    | jdx |
    if idx==jdx
      puts "      Util.ensure(o#{idx} instanceof C#{jdx});"
    else
      puts "      Util.ensure(!(o#{idx} instanceof C#{jdx}));"
    end
  }
  150.times {
    | jdx |
    if jdx>=idx*50 and jdx<idx*50+100
      puts "      Util.ensure(o#{idx} instanceof I#{jdx});"
    else
      puts "      Util.ensure(!(o#{idx} instanceof I#{jdx}));"
    end
  }
}

puts "      System.out.println(\"Assertions all succeeded.\");"

2.times {
  | idx |
  100.times {
    | jdx |
    puts "      ((I#{idx*50+jdx})o#{idx}).foo#{idx*50+jdx}();"
  }
}

puts "      System.out.println(\"Num bucket collisions: \"+CLStats.numBucketCollisions());"
puts "      System.out.println(\"Num itable collisions: \"+CLStats.numItableCollisions());"
puts "      System.out.println(\"Done!\");"
puts "   }"
puts "}"
puts

