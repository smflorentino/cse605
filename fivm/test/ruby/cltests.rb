$LOAD_PATH << "testmake/lib"
require 'testharness'

class CLTests
  def initialize(vm,heapmult)
    @vm=vm
    @heapmult=heapmult
  end
  
  def heapSize(base)
    (base*@heapmult).to_i
  end

  def runLibTest(t)
    t.name << " (#{@vm})"
    t.env["CLASSPATH"]="lib"
    runTest(t)
  end
  
  def runFivmtestTest(t)
    t.name << " (#{@vm})"
    t.env["CLASSPATH"]="lib/fivmtest.jar"
    runTest(t)
  end
  
  def floatToIntTest(input,output)
    t=Test.new("FloatToInt #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.FloatToInt #{input}")
    runFivmtestTest(t)
  end

  def floatToLongTest(input,output)
    t=Test.new("FloatToLong #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.FloatToLong #{input}")
    runFivmtestTest(t)
  end
  
  def floatToDoubleTest(input,output)
    t=Test.new("FloatToDouble #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.FloatToDouble #{input}")
    runFivmtestTest(t)
  end
  
  def doubleToIntTest(input,output)
    t=Test.new("DoubleToInt #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.DoubleToInt #{input}")
    runFivmtestTest(t)
  end

  def doubleToLongTest(input,output)
    t=Test.new("DoubleToLong #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.DoubleToLong #{input}")
    runFivmtestTest(t)
  end
  
  def doubleToFloatTest(input,output)
    t=Test.new("DoubleToFloat #{input} test")
    t.successLine(output);
    t.command("#{@vm} com.fiji.fivm.test.DoubleToFloat #{input}")
    runFivmtestTest(t)
  end
  
  def returnTest(type,input,output)
    t=Test.new("Return#{type} #{input} test")
    t.successLine(output)
    t.command("#{@vm} com.fiji.fivm.test.Return#{type} #{input}")
    runFivmtestTest(t)
  end

  def arrayTest2(type,indexToRead,*elements)
    t=Test.new("#{type}Array #{elements.join(' ')} #{indexToRead} test")
    t.command("#{@vm} com.fiji.fivm.test.#{type}Array #{elements.join(' ')} #{indexToRead}")
    if indexToRead>=0 and indexToRead < elements.length
      t.successLine(yield elements[indexToRead])
    else
      t.successPattern("ArrayIndexOutOfBoundsException")
      t.commands.last.exitStatus=1
    end
    runFivmtestTest(t)
  end
  
  def arrayTests2(type,*elements)
    (-1..(elements.size+1)).each {
      | idx |
      arrayTest2(type,idx,*elements) {
        | x |
        yield x
      }
    }
  end
  
  def arrayTests(type,*elements)
    arrayTests2(type,*elements) { |x| x }
  end

  def simpleOutTest(test,output)
    t=Test.new("#{test} test")
    t.successLine(output)
    t.command("#{@vm} com.fiji.fivm.test.#{test}")
    runFivmtestTest(t)
  end
  
  def yaInstSimpleOutTest(test,output)
    t=Test.new("#{test} test")
    t.successLine("all static fields stored.")
    t.successLine("all instance fields stored.")
    t.successLine(output)
    t.command("#{@vm} com.fiji.fivm.test.#{test}")
    runFivmtestTest(t)
  end
  
  def yaInstNullTest(test)
    t=Test.new("#{test} test")
    t.successPattern("NullPointerException")
    t.failPattern("result =")
    t.failPattern("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.#{test}",
              :exitStatus=>1)
    runFivmtestTest(t)
  end
  
  def yaStaticSimpleOutTest(test,output)
    t=Test.new("#{test} test")
    t.successLine("all static fields stored.")
    t.successLine(output)
    t.command("#{@vm} com.fiji.fivm.test.#{test}")
    runFivmtestTest(t)
  end

  def simpleNullTest(test)
    t=Test.new("#{test} test")
    t.successPattern("NullPointerException")
    t.failPattern("got to here.")
    t.command("#{@vm} com.fiji.fivm.test.#{test}",
              :exitStatus=>1)
    runFivmtestTest(t)
  end

  def simpleInOutTest(test,input,output)
    t=Test.new("#{test} #{input} test")
    t.successLine(output)
    t.command("#{@vm} com.fiji.fivm.test.#{test} #{input}")
    runFivmtestTest(t)
  end
  
  def tableSwitchTest(test,min,max)
    simpleInOutTest(test,min-1000000,2000)
    simpleInOutTest(test,min-1,2000)
    (max-min+1).times {
      | idx |
      simpleInOutTest(test,idx+min,1000+100*idx)
    }
    simpleInOutTest(test,max+1,2000)
    simpleInOutTest(test,max+1000000,2000)
  end
  
  def run
    t=Test.new("simple hello test")
    t.successLine("hello!")
    t.command("#{@vm} hello")
    runLibTest(t)
    
    t=Test.new("Print4 test")
    t.successLine("I'm number four!")
    t.command("#{@vm} com.fiji.fivm.test.Print4")
    runFivmtestTest(t)
    
    t=Test.new("Exit42 test")
    t.successLine("Exiting with status = 42")
    t.command("#{@vm} com.fiji.fivm.test.Exit42",
              :exitStatus=>42)
    runFivmtestTest(t)
    
    t=Test.new("ExitThrow test")
    t.successPattern("Exiting by throwing an exception")
    t.command("#{@vm} com.fiji.fivm.test.ExitThrow",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("RCETest test")
    t.successPattern("That didn't break")
    t.command("#{@vm} com.fiji.fivm.test.RCETest")
    runFivmtestTest(t)
    
    t=Test.new("InstTest test")
    t.successLine("Instantiation of SomeClass worked")
    t.command("#{@vm} com.fiji.fivm.test.InstTest")
    runFivmtestTest(t)
    
    t=Test.new("InstNullTest test")
    t.successLine("got to here.")
    t.successPattern("NullPointerException")
    t.failPattern("SomeClass.method")
    t.command("#{@vm} com.fiji.fivm.test.InstNullTest",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("IntAdd fail test 1")
    t.successPattern("ArrayIndexOutOfBoundsException")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd 1",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("IntAdd fail test 2")
    t.successPattern("ArrayIndexOutOfBoundsException")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("InstIntStoreLoadTest test")
    t.successLine("That worked")
    t.command("#{@vm} com.fiji.fivm.test.InstIntStoreLoadTest")
    runFivmtestTest(t)
    
    t=Test.new("AconstNullTest test")
    t.successLine("null")
    t.command("#{@vm} com.fiji.fivm.test.AconstNullTest")
    runFivmtestTest(t)
    
    t=Test.new("NopTest test")
    t.successLine("About to do a nop...")
    t.successLine("Did a nop, and it didn't break.")
    t.command("#{@vm} com.fiji.fivm.test.NopTest")
    runFivmtestTest(t)
    
    t=Test.new("IconstM1Test test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IconstM1Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst0Test test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.Iconst0Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst1Test test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.Iconst1Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst2Test test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.Iconst2Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst3Test test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.Iconst3Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst4Test test")
    t.successLine("4")
    t.command("#{@vm} com.fiji.fivm.test.Iconst4Test")
    runFivmtestTest(t)
    
    t=Test.new("Iconst5Test test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.Iconst5Test")
    runFivmtestTest(t)
    
    t=Test.new("LdcInt0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LdcInt0")
    runFivmtestTest(t)
    
    t=Test.new("LdcInt42 test")
    t.successLine("42")
    t.command("#{@vm} com.fiji.fivm.test.LdcInt42")
    runFivmtestTest(t)
    
    t=Test.new("LdcInt1000000000 test")
    t.successLine("1000000000")
    t.command("#{@vm} com.fiji.fivm.test.LdcInt1000000000")
    runFivmtestTest(t)
    
    t=Test.new("LdcLong0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LdcLong0")
    runFivmtestTest(t)
    
    t=Test.new("LdcLong42 test")
    t.successLine("42")
    t.command("#{@vm} com.fiji.fivm.test.LdcLong42")
    runFivmtestTest(t)
    
    t=Test.new("LdcLong1000000000000 test")
    t.successLine("1000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LdcLong1000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LdcLongM1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LdcLongM1")
    runFivmtestTest(t)
    
    t=Test.new("IntAdd 1+2 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd 1 2")
    runFivmtestTest(t)
    
    t=Test.new("IntAdd 300+400 test")
    t.successLine("700")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd 300 400")
    runFivmtestTest(t)
    
    t=Test.new("IntAdd 1+-2 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd 1 -2")
    runFivmtestTest(t)
    
    t=Test.new("IntAdd -1+2 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd -1 2")
    runFivmtestTest(t)
    
    t=Test.new("IntAdd -1+-2 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.IntAdd -1 -2")
    runFivmtestTest(t)
    
    t=Test.new("IntSub 2-1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntSub 2 1")
    runFivmtestTest(t)
    
    t=Test.new("IntSub 2- -1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.IntSub 2 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntMul 2*1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntMul 2 1")
    runFivmtestTest(t)
    
    t=Test.new("IntMul 2*0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMul 2 0")
    runFivmtestTest(t)
    
    t=Test.new("IntMul 100*200 test")
    t.successLine("20000")
    t.command("#{@vm} com.fiji.fivm.test.IntMul 100 200")
    runFivmtestTest(t)
    
    t=Test.new("IntMul -100*200 test")
    t.successLine("-20000")
    t.command("#{@vm} com.fiji.fivm.test.IntMul -100 200")
    runFivmtestTest(t)
    
    t=Test.new("IntMul -100*-200 test")
    t.successLine("20000")
    t.command("#{@vm} com.fiji.fivm.test.IntMul -100 -200")
    runFivmtestTest(t)
    
    t=Test.new("IntMul 100*-200 test")
    t.successLine("-20000")
    t.command("#{@vm} com.fiji.fivm.test.IntMul 100 -200")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv 2/1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv 2 1")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv 3/1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv 3 1")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv 42/8 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv 42 8")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv 3/0 test")
    t.successPattern("ArithmeticException")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv 3 0",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("IntDiv 3/-1 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv -3/-1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv -3 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv -3/1 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv -3 1")
    runFivmtestTest(t)
    
    t=Test.new("IntDiv -2147483648/-1 test")
    t.successLine("-2147483648")
    t.command("#{@vm} com.fiji.fivm.test.IntDiv -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod 2%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod 2 1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod 3%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod 3 1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod 42%8 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntMod 42 8")
    runFivmtestTest(t)
    
    t=Test.new("IntMod 3%0 test")
    t.successPattern("ArithmeticException")
    t.command("#{@vm} com.fiji.fivm.test.IntMod 3 0",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("IntMod 3%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod -3%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod -3 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod -3%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod -3 1")
    runFivmtestTest(t)
    
    t=Test.new("IntMod -2147483648%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntMod -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("IntNeg -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntNeg -1")
    runFivmtestTest(t)
    
    t=Test.new("IntNeg 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntNeg 1")
    runFivmtestTest(t)
    
    t=Test.new("IntNeg 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntNeg 0")
    runFivmtestTest(t)
    
    t=Test.new("IntBitNot -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntBitNot -1")
    runFivmtestTest(t)
    
    t=Test.new("IntBitNot 1 test")
    t.successLine("-2")
    t.command("#{@vm} com.fiji.fivm.test.IntBitNot 1")
    runFivmtestTest(t)
    
    t=Test.new("IntBitNot 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntBitNot 0")
    runFivmtestTest(t)
    
    t=Test.new("IntAnd 3&5 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntAnd 3 5")
    runFivmtestTest(t)
    
    t=Test.new("IntAnd -1&7 test")
    t.successLine("7")
    t.command("#{@vm} com.fiji.fivm.test.IntAnd -1 7")
    runFivmtestTest(t)
    
    t=Test.new("IntAnd 256&0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntAnd 256 0")
    runFivmtestTest(t)
    
    t=Test.new("IntOr 3|5 test")
    t.successLine("7")
    t.command("#{@vm} com.fiji.fivm.test.IntOr 3 5")
    runFivmtestTest(t)
    
    t=Test.new("IntOr -1|7 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntOr -1 7")
    runFivmtestTest(t)
    
    t=Test.new("IntOr 256|0 test")
    t.successLine("256")
    t.command("#{@vm} com.fiji.fivm.test.IntOr 256 0")
    runFivmtestTest(t)
    
    t=Test.new("IntXor 3^5 test")
    t.successLine("6")
    t.command("#{@vm} com.fiji.fivm.test.IntXor 3 5")
    runFivmtestTest(t)
    
    t=Test.new("IntXor -1^7 test")
    t.successLine("-8")
    t.command("#{@vm} com.fiji.fivm.test.IntXor -1 7")
    runFivmtestTest(t)
    
    t=Test.new("IntXor 256^0 test")
    t.successLine("256")
    t.command("#{@vm} com.fiji.fivm.test.IntXor 256 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 0<<0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 0<<1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 0 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 0<<2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 0 2")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 0<<31 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 0 31")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 0<<32 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 0 32")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 1<<0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 1 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 1<<1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 1 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 1<<2 test")
    t.successLine("4")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 1 2")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 1<<31 test")
    t.successLine("-2147483648")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 1 31")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 1<<32 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 1 32")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 5<<0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 5 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 5<<1 test")
    t.successLine("10")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 5 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 5<<2 test")
    t.successLine("20")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 5 2")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 5<<31 test")
    t.successLine("-2147483648")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 5 31")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 4<<31 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 4 31")
    runFivmtestTest(t)
    
    t=Test.new("IntShl 5<<32 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntShl 5 32")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 0>>0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 5>>0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 5 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShr -10>>0 test")
    t.successLine("-10")
    t.command("#{@vm} com.fiji.fivm.test.IntShr -10 0")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 0>>1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 0 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 0>>2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 0 2")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 0>>31 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 0 31")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 0>>32 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 0 32")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 11>>1 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 11 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShr 11>>2 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntShr 11 2")
    runFivmtestTest(t)
    
    t=Test.new("IntShr -1>>1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntShr -1 1")
    runFivmtestTest(t)
    
    t=Test.new("IntShr -1>>5 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.IntShr -1 5")
    runFivmtestTest(t)
    
    t=Test.new("IntShr -10>>1 test")
    t.successLine("-5")
    t.command("#{@vm} com.fiji.fivm.test.IntShr -10 1")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 0>>>0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 5>>>0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 5 0")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr -10>>>0 test")
    t.successLine("-10")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr -10 0")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 0>>>1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 0 1")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 0>>>2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 0 2")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 0>>>31 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 0 31")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 0>>>32 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 0 32")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 11>>>1 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 11 1")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr 11>>>2 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr 11 2")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr -1>>>1 test")
    t.successLine("2147483647")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr -1 1")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr -1>>>5 test")
    t.successLine("134217727")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr -1 5")
    runFivmtestTest(t)
    
    t=Test.new("IntUshr -10>>>1 test")
    t.successLine("2147483643")
    t.command("#{@vm} com.fiji.fivm.test.IntUshr -10 1")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 0==0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 42==42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 42==0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 0==42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq -42==-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntEq -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 42==-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq -42==42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq -42==0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntEq 0==-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 0!=0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 42!=42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 42!=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 0!=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq -42!=-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 42!=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq -42!=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq -42!=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq 0!=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 0<0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 42<42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 42<0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 0<42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt -42<-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 42<-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt -42<42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLt -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt -42<0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLt -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLt 0<-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 0>0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 42>42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 42>0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 0>42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt -42>-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 42>-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt -42>42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt -42>0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGt 0>-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGt 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 0<=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 42<=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 42<=0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 0<=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe -42<=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 42<=-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe -42<=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe -42<=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLe 0<=-42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLe 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 0>=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 42>=42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 42>=0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 0>=42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe -42>=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe -42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 42>=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 42 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe -42>=42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGe -42 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe -42>=0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGe -42 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGe 0>=-42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe 0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq0 0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntEq0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntEq0 42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntEq0 -42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntEq0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq0 0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq0 42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntNeq0 -42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntNeq0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt0 0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLt0 42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLt0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLt0 -42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLt0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt0 0 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGt0 42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGt0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGt0 -42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGt0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe0 0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntLe0 42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntLe0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntLe0 -42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntLe0 -42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe0 0 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe0 0")
    runFivmtestTest(t)
    
    t=Test.new("IntGe0 42 test")
    t.successLine("true")
    t.command("#{@vm} com.fiji.fivm.test.IntGe0 42")
    runFivmtestTest(t)
    
    t=Test.new("IntGe0 -42 test")
    t.successLine("false")
    t.command("#{@vm} com.fiji.fivm.test.IntGe0 -42")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd 1+2 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd 1 2")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd 300+400 test")
    t.successLine("700")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd 300 400")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd 1+-2 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd 1 -2")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd -1+2 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd -1 2")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd -1+-2 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd -1 -2")
    runFivmtestTest(t)
    
    t=Test.new("LongAdd 300000000000+400000000000 test")
    t.successLine("700000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongAdd 300000000000 400000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongSub 2-1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongSub 2 1")
    runFivmtestTest(t)
    
    t=Test.new("LongSub 2- -1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.LongSub 2 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongSub 2000000000000-1000000000000 test")
    t.successLine("1000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongSub 2000000000000 1000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongMul 2*1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongMul 2 1")
    runFivmtestTest(t)
    
    t=Test.new("LongMul 2*0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMul 2 0")
    runFivmtestTest(t)
    
    t=Test.new("LongMul 100*200 test")
    t.successLine("20000")
    t.command("#{@vm} com.fiji.fivm.test.LongMul 100 200")
    runFivmtestTest(t)
    
    t=Test.new("LongMul -100*200 test")
    t.successLine("-20000")
    t.command("#{@vm} com.fiji.fivm.test.LongMul -100 200")
    runFivmtestTest(t)
    
    t=Test.new("LongMul -100*-200 test")
    t.successLine("20000")
    t.command("#{@vm} com.fiji.fivm.test.LongMul -100 -200")
    runFivmtestTest(t)
    
    t=Test.new("LongMul 100*-200 test")
    t.successLine("-20000")
    t.command("#{@vm} com.fiji.fivm.test.LongMul 100 -200")
    runFivmtestTest(t)
    
    t=Test.new("LongMul 1000000000*2000000000 test")
    t.successLine("2000000000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongMul 1000000000 2000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv 2/1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv 2 1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv 3/1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv 3 1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv 42/8 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv 42 8")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv 3/0 test")
    t.successPattern("ArithmeticException")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv 3 0",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("LongDiv 3/-1 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv -3/-1 test")
    t.successLine("3")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv -3 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv -3/1 test")
    t.successLine("-3")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv -3 1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv -2147483648/-1 test")
    t.successLine("2147483648")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongDiv -9223372036854775808/-1 test")
    t.successLine("-9223372036854775808")
    t.command("#{@vm} com.fiji.fivm.test.LongDiv -9223372036854775808 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod 2%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod 2 1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod 3%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod 3 1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod 42%8 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongMod 42 8")
    runFivmtestTest(t)
    
    t=Test.new("LongMod 3%0 test")
    t.successPattern("ArithmeticException")
    t.command("#{@vm} com.fiji.fivm.test.LongMod 3 0",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("LongMod 3%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod -3%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod -3 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod -3%1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod -3 1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod -2147483648%-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongMod -9223372036854775808/-1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongMod -9223372036854775808 -1")
    runFivmtestTest(t)
    
    t=Test.new("LongNeg -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongNeg -1")
    runFivmtestTest(t)
    
    t=Test.new("LongNeg 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongNeg 1")
    runFivmtestTest(t)
    
    t=Test.new("LongNeg 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongNeg 0")
    runFivmtestTest(t)
    
    t=Test.new("LongNeg -1000000000000 test")
    t.successLine("1000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongNeg -1000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongBitNot -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongBitNot -1")
    runFivmtestTest(t)
    
    t=Test.new("LongBitNot 1 test")
    t.successLine("-2")
    t.command("#{@vm} com.fiji.fivm.test.LongBitNot 1")
    runFivmtestTest(t)
    
    t=Test.new("LongBitNot 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongBitNot 0")
    runFivmtestTest(t)
    
    t=Test.new("LongBitNot -1000000000000 test")
    t.successLine("999999999999")
    t.command("#{@vm} com.fiji.fivm.test.LongBitNot -1000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd 3&5 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd 3 5")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd -1&7 test")
    t.successLine("7")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd -1 7")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd 256&0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd 256 0")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd 3000000000000&5000000000000 test")
    t.successLine("584739000320")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd 3000000000000 5000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd -1000000000000&7000000000000 test")
    t.successLine("6687264239616")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd -1000000000000 7000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongAnd 256000000000000&0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongAnd 256000000000000 0")
    runFivmtestTest(t)
    
    t=Test.new("LongOr 3|5 test")
    t.successLine("7")
    t.command("#{@vm} com.fiji.fivm.test.LongOr 3 5")
    runFivmtestTest(t)
    
    t=Test.new("LongOr -1|7 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongOr -1 7")
    runFivmtestTest(t)
    
    t=Test.new("LongOr 256|0 test")
    t.successLine("256")
    t.command("#{@vm} com.fiji.fivm.test.LongOr 256 0")
    runFivmtestTest(t)
    
    t=Test.new("LongOr 3000000000000|5000000000000 test")
    t.successLine("7415260999680")
    t.command("#{@vm} com.fiji.fivm.test.LongOr 3000000000000 5000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongOr -1000000000000|7000000000000 test")
    t.successLine("-687264239616")
    t.command("#{@vm} com.fiji.fivm.test.LongOr -1000000000000 7000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongOr 256000000000000|0 test")
    t.successLine("256000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongOr 256000000000000 0")
    runFivmtestTest(t)
    
    t=Test.new("LongXor 3^5 test")
    t.successLine("6")
    t.command("#{@vm} com.fiji.fivm.test.LongXor 3 5")
    runFivmtestTest(t)
    
    t=Test.new("LongXor -1^7 test")
    t.successLine("-8")
    t.command("#{@vm} com.fiji.fivm.test.LongXor -1 7")
    runFivmtestTest(t)
    
    t=Test.new("LongXor 256^0 test")
    t.successLine("256")
    t.command("#{@vm} com.fiji.fivm.test.LongXor 256 0")
    runFivmtestTest(t)
    
    t=Test.new("LongXor 3000000000000^5000000000000 test")
    t.successLine("6830521999360")
    t.command("#{@vm} com.fiji.fivm.test.LongXor 3000000000000 5000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongXor -1000000000000^7000000000000 test")
    t.successLine("-7374528479232")
    t.command("#{@vm} com.fiji.fivm.test.LongXor -1000000000000 7000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LongXor 256000000000000^0 test")
    t.successLine("256000000000000")
    t.command("#{@vm} com.fiji.fivm.test.LongXor 256000000000000 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 0<<0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 0 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 0<<1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 0 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 0<<2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 0 2")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 0<<63 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 0 63")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 0<<64 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 0 64")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 1<<0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 1 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 1<<1 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 1 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 1<<2 test")
    t.successLine("4")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 1 2")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 1<<63 test")
    t.successLine("-9223372036854775808")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 1 63")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 1<<64 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 1 64")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 5<<0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 5 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 5<<1 test")
    t.successLine("10")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 5 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 5<<2 test")
    t.successLine("20")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 5 2")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 5<<63 test")
    t.successLine("-9223372036854775808")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 5 63")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 4<<63 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 4 63")
    runFivmtestTest(t)
    
    t=Test.new("LongShl 5<<64 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongShl 5 64")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 0>>0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 0 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 5>>0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 5 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShr -10>>0 test")
    t.successLine("-10")
    t.command("#{@vm} com.fiji.fivm.test.LongShr -10 0")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 0>>1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 0 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 0>>2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 0 2")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 0>>63 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 0 63")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 0>>64 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 0 64")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 11>>1 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 11 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShr 11>>2 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongShr 11 2")
    runFivmtestTest(t)
    
    t=Test.new("LongShr -1>>1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongShr -1 1")
    runFivmtestTest(t)
    
    t=Test.new("LongShr -1>>5 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LongShr -1 5")
    runFivmtestTest(t)
    
    t=Test.new("LongShr -10>>1 test")
    t.successLine("-5")
    t.command("#{@vm} com.fiji.fivm.test.LongShr -10 1")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 0>>>0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 0 0")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 5>>>0 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 5 0")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr -10>>>0 test")
    t.successLine("-10")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr -10 0")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 0>>>1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 0 1")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 0>>>2 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 0 2")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 0>>>31 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 0 31")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 0>>>32 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 0 32")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 11>>>1 test")
    t.successLine("5")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 11 1")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr 11>>>2 test")
    t.successLine("2")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr 11 2")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr -1>>>1 test")
    t.successLine("9223372036854775807")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr -1 1")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr -1>>>5 test")
    t.successLine("576460752303423487")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr -1 5")
    runFivmtestTest(t)
    
    t=Test.new("LongUshr -10>>>1 test")
    t.successLine("9223372036854775803")
    t.command("#{@vm} com.fiji.fivm.test.LongUshr -10 1")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest 0<->0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest 0 0")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest 10000000000000<->10000000000000 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest 10000000000000 10000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest -10000000000000<->-10000000000000 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest -10000000000000 -10000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest -10000000000000<->10000000000000 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest -10000000000000 10000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest 10000000000000<->-10000000000000 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest 10000000000000 -10000000000000")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest 10000000000000<->-9999999999999 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest 10000000000000 -9999999999999")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest -10000000000000<->9999999999999 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest -10000000000000 9999999999999")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest -10000000000000<->-9999999999999 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest -10000000000000 -9999999999999")
    runFivmtestTest(t)
    
    t=Test.new("LcmpTest 10000000000000<->9999999999999 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.LcmpTest 10000000000000 9999999999999")
    runFivmtestTest(t)
    
    t=Test.new("FloatAdd 1+2 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatAdd 1 2")
    runFivmtestTest(t)
    
    t=Test.new("FloatAdd 300+400 test")
    t.successLine("700.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatAdd 300 400")
    runFivmtestTest(t)
    
    t=Test.new("FloatAdd 1+-2 test")
    t.successLine("-1.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatAdd 1 -2")
    runFivmtestTest(t)
    
    t=Test.new("FloatAdd -1+2 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatAdd -1 2")
    runFivmtestTest(t)
    
    t=Test.new("FloatAdd -1+-2 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatAdd -1 -2")
    runFivmtestTest(t)
    
    t=Test.new("FloatSub 2-1 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatSub 2 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatSub 2- -1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatSub 2 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul 2*1 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul 2 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul 2*0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul 2 0")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul 100*200 test")
    t.successLine("20000.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul 100 200")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul -100*200 test")
    t.successLine("-20000.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul -100 200")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul -100*-200 test")
    t.successLine("20000.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul -100 -200")
    runFivmtestTest(t)
    
    t=Test.new("FloatMul 100*-200 test")
    t.successLine("-20000.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMul 100 -200")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv 2/1 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv 2 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv 3/1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv 3 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv 42/8 test")
    t.successLine("5.25")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv 42 8")
    runFivmtestTest(t)

    t=Test.new("FloatDiv 3/0 test")
    t.successLine("Infinity")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv 3 0")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv 3/-1 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv -3/-1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv -3 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv -3/1 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv -3 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatDiv -2147483648/-1 test")
    t.successLine("2.14748365E9")
    t.command("#{@vm} com.fiji.fivm.test.FloatDiv -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod 2%1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod 2 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod 3%1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod 3 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod 42%8 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod 42 8")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod 3%0 test")
    t.successLine("NaN")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod 3 0")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod 3%-1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod 3 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod -3%-1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod -3 -1")
    runFivmtestTest(t)

    t=Test.new("FloatMod -3%1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod -3 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatMod -2147483648%-1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatMod -2147483648 -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatNeg -1 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatNeg -1")
    runFivmtestTest(t)
    
    t=Test.new("FloatNeg 1 test")
    t.successLine("-1.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatNeg 1")
    runFivmtestTest(t)
    
    t=Test.new("FloatNeg 0 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.FloatNeg 0")
    runFivmtestTest(t)
    
    t=Test.new("Lconst0Test test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.Lconst0Test")
    runFivmtestTest(t)
    
    t=Test.new("Lconst1Test test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.Lconst1Test")
    runFivmtestTest(t)
    
    t=Test.new("DoubleAdd 1+2 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleAdd 1 2")
    runFivmtestTest(t)
    
    t=Test.new("DoubleAdd 300+400 test")
    t.successLine("700.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleAdd 300 400")
    runFivmtestTest(t)
    
    t=Test.new("DoubleAdd 1+-2 test")
    t.successLine("-1.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleAdd 1 -2")
    runFivmtestTest(t)
    
    t=Test.new("DoubleAdd -1+2 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleAdd -1 2")
    runFivmtestTest(t)
    
    t=Test.new("DoubleAdd -1+-2 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleAdd -1 -2")
    runFivmtestTest(t)
    
    t=Test.new("DoubleSub 2-1 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleSub 2 1")
    runFivmtestTest(t)
    
    t=Test.new("DoubleSub 2- -1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleSub 2 -1")
    runFivmtestTest(t)
    
    t=Test.new("DoubleMul 2*1 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul 2 1")
    runFivmtestTest(t)
    
    t=Test.new("DoubleMul 2*0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul 2 0")
    runFivmtestTest(t)
    
    t=Test.new("DoubleMul 100*200 test")
    t.successLine("20000.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul 100 200")
    runFivmtestTest(t)
    
    t=Test.new("DoubleMul -100*200 test")
    t.successLine("-20000.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul -100 200")
    runFivmtestTest(t)

    t=Test.new("DoubleMul -100*-200 test")
    t.successLine("20000.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul -100 -200")
    runFivmtestTest(t)

    t=Test.new("DoubleMul 100*-200 test")
    t.successLine("-20000.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMul 100 -200")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv 2/1 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv 2 1")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv 3/1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv 3 1")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv 42/8 test")
    t.successLine("5.25")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv 42 8")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv 3/0 test")
    t.successLine("Infinity")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv 3 0")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv 3/-1 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv 3 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv -3/-1 test")
    t.successLine("3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv -3 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv -3/1 test")
    t.successLine("-3.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv -3 1")
    runFivmtestTest(t)

    t=Test.new("DoubleDiv -2147483648/-1 test")
    t.successLine("2.147483648E9")
    t.command("#{@vm} com.fiji.fivm.test.DoubleDiv -2147483648 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod 2%1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod 2 1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod 3%1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod 3 1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod 42%8 test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod 42 8")
    runFivmtestTest(t)

    t=Test.new("DoubleMod 3%0 test")
    t.successLine("NaN")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod 3 0")
    runFivmtestTest(t)

    t=Test.new("DoubleMod 3%-1 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod 3 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod -3%-1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod -3 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod -3%1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod -3 1")
    runFivmtestTest(t)

    t=Test.new("DoubleMod -2147483648%-1 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleMod -2147483648 -1")
    runFivmtestTest(t)

    t=Test.new("DoubleNeg -1 test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleNeg -1")
    runFivmtestTest(t)

    t=Test.new("DoubleNeg 1 test")
    t.successLine("-1.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleNeg 1")
    runFivmtestTest(t)

    t=Test.new("DoubleNeg 0 test")
    t.successLine("-0.0")
    t.command("#{@vm} com.fiji.fivm.test.DoubleNeg 0")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 0 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 0 0")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 1 0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 1 0")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 0 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 0 1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest -1 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest -1 0")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 0 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 0 -1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 1 1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 1 1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest 1 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest 1 -1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest -1 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest -1 1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest -1 -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest -1 -1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 0 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 0 0")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 1 0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 1 0")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 0 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 0 1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest -1 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest -1 0")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 0 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 0 -1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 1 1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 1 1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest 1 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest 1 -1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest -1 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest -1 1")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest -1 -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest -1 -1")
    runFivmtestTest(t)

    t=Test.new("FcmplTest NaN NaN test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.FcmplTest NaN NaN")
    runFivmtestTest(t)

    t=Test.new("FcmpgTest NaN NaN test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.FcmpgTest NaN NaN")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 0 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 0 0")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 1 0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 1 0")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 0 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 0 1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest -1 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest -1 0")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 0 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 0 -1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 1 1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 1 1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest 1 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest 1 -1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest -1 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest -1 1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest -1 -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest -1 -1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 0 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 0 0")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 1 0 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 1 0")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 0 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 0 1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest -1 0 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest -1 0")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 0 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 0 -1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 1 1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 1 1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest 1 -1 test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest 1 -1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest -1 1 test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest -1 1")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest -1 -1 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest -1 -1")
    runFivmtestTest(t)

    t=Test.new("DcmplTest NaN NaN test")
    t.successLine("-1")
    t.command("#{@vm} com.fiji.fivm.test.DcmplTest NaN NaN")
    runFivmtestTest(t)

    t=Test.new("DcmpgTest NaN NaN test")
    t.successLine("1")
    t.command("#{@vm} com.fiji.fivm.test.DcmpgTest NaN NaN")
    runFivmtestTest(t)

    t=Test.new("Fconst0Test test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.Fconst0Test")
    runFivmtestTest(t)

    t=Test.new("Fconst1Test test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.Fconst1Test")
    runFivmtestTest(t)

    t=Test.new("Fconst2Test test")
    t.successLine("2.0")
    t.command("#{@vm} com.fiji.fivm.test.Fconst2Test")
    runFivmtestTest(t)

    t=Test.new("Dconst0Test test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.Dconst0Test")
    runFivmtestTest(t)

    t=Test.new("Dconst1Test test")
    t.successLine("1.0")
    t.command("#{@vm} com.fiji.fivm.test.Dconst1Test")
    runFivmtestTest(t)

    t=Test.new("LdcFloat0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.LdcFloat0")
    runFivmtestTest(t)

    t=Test.new("LdcFloatPi test")
    t.successLine("3.1415927")
    t.command("#{@vm} com.fiji.fivm.test.LdcFloatPi")
    runFivmtestTest(t)

    t=Test.new("LdcDouble0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.LdcDouble0")
    runFivmtestTest(t)

    t=Test.new("LdcDoublePi test")
    t.successLine("3.1415927410125732")
    t.command("#{@vm} com.fiji.fivm.test.LdcDoublePi")
    runFivmtestTest(t)

    t=Test.new("IntToByte 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte 0")
    runFivmtestTest(t)

    t=Test.new("IntToByte 100 test")
    t.successLine("100")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte 100")
    runFivmtestTest(t)

    t=Test.new("IntToByte -100 test")
    t.successLine("-100")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte -100")
    runFivmtestTest(t)

    t=Test.new("IntToByte 10000 test")
    t.successLine("16")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte 10000")
    runFivmtestTest(t)

    t=Test.new("IntToByte -10000 test")
    t.successLine("-16")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte -10000")
    runFivmtestTest(t)

    t=Test.new("IntToByte 1000000 test")
    t.successLine("64")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToByte -1000000 test")
    t.successLine("-64")
    t.command("#{@vm} com.fiji.fivm.test.IntToByte -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToShort 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort 0")
    runFivmtestTest(t)

    t=Test.new("IntToShort 100 test")
    t.successLine("100")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort 100")
    runFivmtestTest(t)

    t=Test.new("IntToShort -100 test")
    t.successLine("-100")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort -100")
    runFivmtestTest(t)

    t=Test.new("IntToShort 10000 test")
    t.successLine("10000")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort 10000")
    runFivmtestTest(t)

    t=Test.new("IntToShort -10000 test")
    t.successLine("-10000")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort -10000")
    runFivmtestTest(t)

    t=Test.new("IntToShort 1000000 test")
    t.successLine("16960")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToShort -1000000 test")
    t.successLine("-16960")
    t.command("#{@vm} com.fiji.fivm.test.IntToShort -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToChar 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar 0")
    runFivmtestTest(t)

    t=Test.new("IntToChar 100 test")
    t.successLine("100")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar 100")
    runFivmtestTest(t)

    t=Test.new("IntToChar -100 test")
    t.successLine("65436")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar -100")
    runFivmtestTest(t)

    t=Test.new("IntToChar 10000 test")
    t.successLine("10000")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar 10000")
    runFivmtestTest(t)

    t=Test.new("IntToChar -10000 test")
    t.successLine("55536")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar -10000")
    runFivmtestTest(t)

    t=Test.new("IntToChar 1000000 test")
    t.successLine("16960")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToChar -1000000 test")
    t.successLine("48576")
    t.command("#{@vm} com.fiji.fivm.test.IntToChar -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToLong 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong 0")
    runFivmtestTest(t)

    t=Test.new("IntToLong 100 test")
    t.successLine("100")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong 100")
    runFivmtestTest(t)

    t=Test.new("IntToLong -100 test")
    t.successLine("-100")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong -100")
    runFivmtestTest(t)

    t=Test.new("IntToLong 10000 test")
    t.successLine("10000")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong 10000")
    runFivmtestTest(t)

    t=Test.new("IntToLong -10000 test")
    t.successLine("-10000")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong -10000")
    runFivmtestTest(t)

    t=Test.new("IntToLong 1000000 test")
    t.successLine("1000000")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToLong -1000000 test")
    t.successLine("-1000000")
    t.command("#{@vm} com.fiji.fivm.test.IntToLong -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToFloat 0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat 0")
    runFivmtestTest(t)

    t=Test.new("IntToFloat 100 test")
    t.successLine("100.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat 100")
    runFivmtestTest(t)

    t=Test.new("IntToFloat -100 test")
    t.successLine("-100.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat -100")
    runFivmtestTest(t)

    t=Test.new("IntToFloat 10000 test")
    t.successLine("10000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat 10000")
    runFivmtestTest(t)

    t=Test.new("IntToFloat -10000 test")
    t.successLine("-10000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat -10000")
    runFivmtestTest(t)

    t=Test.new("IntToFloat 1000000 test")
    t.successLine("1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToFloat -1000000 test")
    t.successLine("-1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToFloat 1999999999 test")
    t.successLine("2.0E9")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat 1999999999")
    runFivmtestTest(t)

    t=Test.new("IntToFloat -1999999999 test")
    t.successLine("-2.0E9")
    t.command("#{@vm} com.fiji.fivm.test.IntToFloat -1999999999")
    runFivmtestTest(t)

    t=Test.new("IntToDouble 0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble 0")
    runFivmtestTest(t)

    t=Test.new("IntToDouble 100 test")
    t.successLine("100.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble 100")
    runFivmtestTest(t)

    t=Test.new("IntToDouble -100 test")
    t.successLine("-100.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble -100")
    runFivmtestTest(t)

    t=Test.new("IntToDouble 10000 test")
    t.successLine("10000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble 10000")
    runFivmtestTest(t)

    t=Test.new("IntToDouble -10000 test")
    t.successLine("-10000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble -10000")
    runFivmtestTest(t)

    t=Test.new("IntToDouble 1000000 test")
    t.successLine("1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble 1000000")
    runFivmtestTest(t)

    t=Test.new("IntToDouble -1000000 test")
    t.successLine("-1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble -1000000")
    runFivmtestTest(t)

    t=Test.new("IntToDouble 1999999999 test")
    t.successLine("1.999999999E9")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble 1999999999")
    runFivmtestTest(t)

    t=Test.new("IntToDouble -1999999999 test")
    t.successLine("-1.999999999E9")
    t.command("#{@vm} com.fiji.fivm.test.IntToDouble -1999999999")
    runFivmtestTest(t)

    t=Test.new("LongToInt 0 test")
    t.successLine("0")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt 0")
    runFivmtestTest(t)

    t=Test.new("LongToInt 100 test")
    t.successLine("100")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt 100")
    runFivmtestTest(t)

    t=Test.new("LongToInt -100 test")
    t.successLine("-100")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt -100")
    runFivmtestTest(t)

    t=Test.new("LongToInt 10000 test")
    t.successLine("10000")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt 10000")
    runFivmtestTest(t)

    t=Test.new("LongToInt -10000 test")
    t.successLine("-10000")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt -10000")
    runFivmtestTest(t)

    t=Test.new("LongToInt 1000000 test")
    t.successLine("1000000")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt 1000000")
    runFivmtestTest(t)

    t=Test.new("LongToInt -1000000 test")
    t.successLine("-1000000")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt -1000000")
    runFivmtestTest(t)

    t=Test.new("LongToInt 1000000000000 test")
    t.successLine("-727379968")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt 1000000000000")
    runFivmtestTest(t)

    t=Test.new("LongToInt -1000000000000 test")
    t.successLine("727379968")
    t.command("#{@vm} com.fiji.fivm.test.LongToInt -1000000000000")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 0")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 100 test")
    t.successLine("100.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 100")
    runFivmtestTest(t)

    t=Test.new("LongToFloat -100 test")
    t.successLine("-100.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat -100")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 10000 test")
    t.successLine("10000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 10000")
    runFivmtestTest(t)

    t=Test.new("LongToFloat -10000 test")
    t.successLine("-10000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat -10000")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 1000000 test")
    t.successLine("1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 1000000")
    runFivmtestTest(t)

    t=Test.new("LongToFloat -1000000 test")
    t.successLine("-1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat -1000000")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 1999999999 test")
    t.successLine("2.0E9")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 1999999999")
    runFivmtestTest(t)

    t=Test.new("LongToFloat -1999999999 test")
    t.successLine("-2.0E9")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat -1999999999")
    runFivmtestTest(t)

    t=Test.new("LongToFloat 1999999999999 test")
    t.successLine("1.99999999E12")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat 1999999999999")
    runFivmtestTest(t)

    t=Test.new("LongToFloat -1999999999999 test")
    t.successLine("-1.99999999E12")
    t.command("#{@vm} com.fiji.fivm.test.LongToFloat -1999999999999")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 0 test")
    t.successLine("0.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 0")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 100 test")
    t.successLine("100.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 100")
    runFivmtestTest(t)

    t=Test.new("LongToDouble -100 test")
    t.successLine("-100.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble -100")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 10000 test")
    t.successLine("10000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 10000")
    runFivmtestTest(t)

    t=Test.new("LongToDouble -10000 test")
    t.successLine("-10000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble -10000")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 1000000 test")
    t.successLine("1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 1000000")
    runFivmtestTest(t)

    t=Test.new("LongToDouble -1000000 test")
    t.successLine("-1000000.0")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble -1000000")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 1999999999 test")
    t.successLine("1.999999999E9")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 1999999999")
    runFivmtestTest(t)

    t=Test.new("LongToDouble -1999999999 test")
    t.successLine("-1.999999999E9")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble -1999999999")
    runFivmtestTest(t)

    t=Test.new("LongToDouble 1999999999999 test")
    t.successLine("1.999999999999E12")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble 1999999999999")
    runFivmtestTest(t)

    t=Test.new("LongToDouble -1999999999999 test")
    t.successLine("-1.999999999999E12")
    t.command("#{@vm} com.fiji.fivm.test.LongToDouble -1999999999999")
    runFivmtestTest(t)

    floatToIntTest("0","0")
    floatToIntTest("1.5","1")
    floatToIntTest("-1.5","-1")
    floatToIntTest("65426.357287","65426")
    floatToIntTest("-65426.357287","-65426")
    floatToIntTest("NaN","0")
    floatToIntTest("Infinity","2147483647")
    floatToIntTest("-Infinity","-2147483648")
    
    floatToLongTest("0","0")
    floatToLongTest("1.5","1")
    floatToLongTest("-1.5","-1")
    floatToLongTest("65426.357287","65426")
    floatToLongTest("-65426.357287","-65426")
    floatToLongTest("NaN","0")
    floatToLongTest("Infinity","9223372036854775807")
    floatToLongTest("-Infinity","-9223372036854775808")
    
    floatToDoubleTest("0.0","0.0")
    floatToDoubleTest("1.0","1.0")
    floatToDoubleTest("-1.0","-1.0")
    floatToDoubleTest("1.5","1.5")
    floatToDoubleTest("-1.5","-1.5")
    floatToDoubleTest("54734773527523.462363526523","5.4734773813248E13")
    floatToDoubleTest("-54734773527523.462363526523","-5.4734773813248E13")

    doubleToIntTest("0","0")
    doubleToIntTest("1.5","1")
    doubleToIntTest("-1.5","-1")
    doubleToIntTest("65426.357287","65426")
    doubleToIntTest("-65426.357287","-65426")
    doubleToIntTest("NaN","0")
    doubleToIntTest("Infinity","2147483647")
    doubleToIntTest("-Infinity","-2147483648")
    
    doubleToLongTest("0","0")
    doubleToLongTest("1.5","1")
    doubleToLongTest("-1.5","-1")
    doubleToLongTest("65426.357287","65426")
    doubleToLongTest("-65426.357287","-65426")
    doubleToLongTest("NaN","0")
    doubleToLongTest("Infinity","9223372036854775807")
    doubleToLongTest("-Infinity","-9223372036854775808")
    
    doubleToFloatTest("0.0","0.0")
    doubleToFloatTest("1.0","1.0")
    doubleToFloatTest("-1.0","-1.0")
    doubleToFloatTest("1.5","1.5")
    doubleToFloatTest("-1.5","-1.5")
    doubleToFloatTest("54734773527523.462363526523","5.4734774E13")
    doubleToFloatTest("-54734773527523.462363526523","-5.4734774E13")
    
    t=Test.new("InstOtherTest test")
    t.successLine("Instantiation of SomeInterfaceImpl worked")
    t.command("#{@vm} com.fiji.fivm.test.InstOtherTest")
    runFivmtestTest(t)

    t=Test.new("InstOtherTest2 test")
    t.successLine("someInterfaceImpl.someInterfaceMethod() called")
    t.successLine("Instantiation of SomeInterfaceImpl worked")
    t.command("#{@vm} com.fiji.fivm.test.InstOtherTest2")
    runFivmtestTest(t)
    
    returnTest("Boolean","true","false")
    returnTest("Boolean","false","true")

    returnTest("Byte","0","1")
    returnTest("Byte","127","-128")

    returnTest("Char","0","1")
    returnTest("Char","65535","0")

    returnTest("Short","0","1")
    returnTest("Short","32767","-32768")

    returnTest("Int","0","1")
    returnTest("Int","2147483647","-2147483648")
    
    returnTest("Long","0","1")
    returnTest("Long","9223372036854775807","-9223372036854775808")
    
    returnTest("Float","0","1.0")
    returnTest("Float","10000000","1.0000001E7")
    returnTest("Float","1000000000000","1.0E12")
    
    returnTest("Double","0","1.0")
    returnTest("Double","10000000","1.0000001E7")
    returnTest("Double","1000000000000","1.000000000001E12")
    returnTest("Double","10000000000000000","1.0E16")

    5.times {
      | idx |
      t=Test.new("InterfaceCollisionTest test#{idx}")
      t.successPattern("First allocation: com.fiji.fivm.test.InterfaceCollisionTest$C01")
      t.successPattern("Second allocation: com.fiji.fivm.test.InterfaceCollisionTest$C02")
      t.successLine("Assertions all succeeded.")
      (1..9).each {
        | x |
        t.successLine("C01.foo%02d called"%[x])
      }
      (2..10).each {
        | x |
        t.successLine("C02.foo%02d called"%[x])
      }
      t.successLine("Done!")
      t.command("#{@vm} com.fiji.fivm.test.InterfaceCollisionTest")
      runFivmtestTest(t)
    }
    
    arrayTests("Byte")
    arrayTests("Byte",42)
    arrayTests("Byte",59)
    arrayTests("Byte",59,42)
    arrayTests("Byte",5,1,49)
    arrayTests("Byte",-5,1,-49)
    
    arrayTests("Short")
    arrayTests("Short",42)
    arrayTests("Short",59)
    arrayTests("Short",59,42)
    arrayTests("Short",5,1,49)
    arrayTests("Short",32000,-20000,42)
    
    arrayTests("Char")
    arrayTests("Char",42000)
    arrayTests("Char",59)
    arrayTests("Char",59,42)
    arrayTests("Char",5,1,49)
    arrayTests("Char",32000,20000,42)
    
    arrayTests("Int")
    arrayTests("Int",42)
    arrayTests("Int",59)
    arrayTests("Int",59,42)
    arrayTests("Int",5,1,49)
    arrayTests("Int",32000,-20000,42)
    arrayTests("Int",32000000,-20000000,42000)
    
    arrayTests("Long")
    arrayTests("Long",42)
    arrayTests("Long",59)
    arrayTests("Long",59,42)
    arrayTests("Long",5,1,49)
    arrayTests("Long",32000,-20000,42)
    arrayTests("Long",32000000,-20000000,42000)
    arrayTests("Long",32000000000,-200000000000,42000)
    
    # FIXME: float and double array tests?  those would be nice...
    
    ["String","Object"].each {
      | type |
      arrayTests(type)
      arrayTests(type,"abc")
      arrayTests(type,"bca")
      arrayTests(type,"foo","bar")
      arrayTests(type,"blah","bleh","blih")
      arrayTests(type,"gsdajlgsa","grbwhigfhgsa","lkjghjlgsa")
    }
    
    t=Test.new("PopTest test")
    t.successLine("42")
    t.command("#{@vm} com.fiji.fivm.test.PopTest")
    runFivmtestTest(t)
    
    t=Test.new("Pop2Test test")
    t.successLine("42")
    t.command("#{@vm} com.fiji.fivm.test.Pop2Test")
    runFivmtestTest(t)
    
    t=Test.new("DupTest test")
    t.successLine("42 42")
    t.command("#{@vm} com.fiji.fivm.test.DupTest")
    runFivmtestTest(t)
    
    t=Test.new("DupTest2 test")
    t.successLine("42 42")
    t.command("#{@vm} com.fiji.fivm.test.DupTest2")
    runFivmtestTest(t)

    t=Test.new("Dup2Test test")
    t.successLine("72 42 72 42")
    t.command("#{@vm} com.fiji.fivm.test.Dup2Test")
    runFivmtestTest(t)
    
    t=Test.new("Dup2Test2 test")
    t.successLine("72 42 72 42")
    t.command("#{@vm} com.fiji.fivm.test.Dup2Test2")
    runFivmtestTest(t)
    
    t=Test.new("DupX1Test test")
    t.successLine("72 42 72")
    t.command("#{@vm} com.fiji.fivm.test.DupX1Test")
    runFivmtestTest(t)
    
    t=Test.new("DupX1Test2 test")
    t.successLine("72 42 72")
    t.command("#{@vm} com.fiji.fivm.test.DupX1Test2")
    runFivmtestTest(t)
    
    t=Test.new("DupX2Test test")
    t.successLine("23 72 42 23")
    t.command("#{@vm} com.fiji.fivm.test.DupX2Test")
    runFivmtestTest(t)
    
    t=Test.new("DupX2Test2 test")
    t.successLine("23 72 42 23")
    t.command("#{@vm} com.fiji.fivm.test.DupX2Test2")
    runFivmtestTest(t)
    
    t=Test.new("Dup2X1Test test")
    t.successLine("3 2 1 3 2")
    t.command("#{@vm} com.fiji.fivm.test.Dup2X1Test")
    runFivmtestTest(t)
    
    t=Test.new("Dup2X1Test2 test")
    t.successLine("3 2 1 3 2")
    t.command("#{@vm} com.fiji.fivm.test.Dup2X1Test2")
    runFivmtestTest(t)
    
    t=Test.new("Dup2X2Test test")
    t.successLine("4 3 2 1 4 3")
    t.command("#{@vm} com.fiji.fivm.test.Dup2X2Test")
    runFivmtestTest(t)
    
    t=Test.new("Dup2X2Test2 test")
    t.successLine("4 3 2 1 4 3")
    t.command("#{@vm} com.fiji.fivm.test.Dup2X2Test2")
    runFivmtestTest(t)
    
    t=Test.new("ArrayLength 0 test")
    t.successLine(0)
    t.command("#{@vm} com.fiji.fivm.test.ArrayLength")
    runFivmtestTest(t)
    
    t=Test.new("ArrayLength 1 test")
    t.successLine(1)
    t.command("#{@vm} com.fiji.fivm.test.ArrayLength foo")
    runFivmtestTest(t)
    
    t=Test.new("ArrayLength 2 test")
    t.successLine(2)
    t.command("#{@vm} com.fiji.fivm.test.ArrayLength foo bar")
    runFivmtestTest(t)
    
    t=Test.new("ArrayLength 12 test")
    t.successLine(12)
    t.command("#{@vm} com.fiji.fivm.test.ArrayLength foo bar baz blah bleh a b c d e f g")
    runFivmtestTest(t)
    
    5.times {
      | idx |
      t=Test.new("MonitorEnterExit test#{idx}")
      t.successPattern("About to lock java.lang.Object")
      t.successPattern("Locked java.lang.Object")
      t.successPattern("Unlocked java.lang.Object")
      t.successPattern("About to lock a second time java.lang.Object")
      t.successPattern("Locked a second time java.lang.Object")
      t.successPattern("Unlocked a second time java.lang.Object")
      t.command("#{@vm} com.fiji.fivm.test.MonitorEnterExit")
      runFivmtestTest(t)
    }
    
    5.times {
      | idx |
      t=Test.new("MonitorEnterExit2 test#{idx}")
      t.successPattern("About to lock java.lang.Object")
      t.successPattern("Locked java.lang.Object")
      t.successPattern("Unlocked java.lang.Object")
      t.successPattern("About to lock a second time java.lang.Object")
      t.successPattern("Locked a second time java.lang.Object")
      t.successPattern("Unlocked a second time java.lang.Object")
      t.command("#{@vm} com.fiji.fivm.test.MonitorEnterExit2")
      runFivmtestTest(t)
    }
    
    5.times {
      | idx |
      t=Test.new("MonitorEnterExit3 test#{idx}")
      t.successPattern("About to lock java.lang.Object")
      t.successPattern("Locked java.lang.Object")
      t.successPattern("Locked again java.lang.Object")
      t.successPattern("Back to one lock level java.lang.Object")
      t.successPattern("Unlocked java.lang.Object")
      t.successPattern("About to lock a second time java.lang.Object")
      t.successPattern("Locked a second time java.lang.Object")
      t.successPattern("Locked a second time again java.lang.Object")
      t.successPattern("Back to one lock level a second time java.lang.Object")
      t.successPattern("Unlocked a second time java.lang.Object")
      t.command("#{@vm} com.fiji.fivm.test.MonitorEnterExit3")
      runFivmtestTest(t)
    }
    
    simpleOutTest("GetIntFieldTest",42)
    simpleOutTest("GetLongFieldTest",65)
    simpleOutTest("GetFloatFieldTest","5.4")
    simpleOutTest("GetDoubleFieldTest","10.3")
    simpleOutTest("GetObjectFieldTest","foo")
    simpleOutTest("GetIntStaticTest",43)
    simpleOutTest("GetLongStaticTest",65)
    simpleOutTest("GetFloatStaticTest","5.5")
    simpleOutTest("GetDoubleStaticTest","10.4")
    simpleOutTest("GetObjectStaticTest","bar")

    yaInstSimpleOutTest("YAGetBooleanFieldTest1","true")
    yaInstSimpleOutTest("YAGetBooleanFieldTest2","false")
    yaInstSimpleOutTest("YAGetByteFieldTest","41")
    yaInstSimpleOutTest("YAGetCharFieldTest1","200")
    yaInstSimpleOutTest("YAGetCharFieldTest2","55000")
    yaInstSimpleOutTest("YAGetShortFieldTest1","200")
    yaInstSimpleOutTest("YAGetShortFieldTest2","-20000")
    yaInstSimpleOutTest("YAGetIntFieldTest","42000000")
    yaInstSimpleOutTest("YAGetLongFieldTest","65000000000")
    yaInstSimpleOutTest("YAGetFloatFieldTest","5.425")
    yaInstSimpleOutTest("YAGetDoubleFieldTest","10.347421")
    yaInstSimpleOutTest("YAGetObjectFieldTest","foo")
    
    yaInstNullTest("NullYAGetBooleanFieldTest1")
    yaInstNullTest("NullYAGetBooleanFieldTest2")
    yaInstNullTest("NullYAGetByteFieldTest")
    yaInstNullTest("NullYAGetCharFieldTest1")
    yaInstNullTest("NullYAGetCharFieldTest2")
    yaInstNullTest("NullYAGetShortFieldTest1")
    yaInstNullTest("NullYAGetShortFieldTest2")
    yaInstNullTest("NullYAGetIntFieldTest")
    yaInstNullTest("NullYAGetLongFieldTest")
    yaInstNullTest("NullYAGetFloatFieldTest")
    yaInstNullTest("NullYAGetDoubleFieldTest")
    yaInstNullTest("NullYAGetObjectFieldTest")
    
    yaInstNullTest("NullYAPutBooleanFieldTest1")
    yaInstNullTest("NullYAPutBooleanFieldTest2")
    yaInstNullTest("NullYAPutByteFieldTest")
    yaInstNullTest("NullYAPutCharFieldTest1")
    yaInstNullTest("NullYAPutCharFieldTest2")
    yaInstNullTest("NullYAPutShortFieldTest1")
    yaInstNullTest("NullYAPutShortFieldTest2")
    yaInstNullTest("NullYAPutIntFieldTest")
    yaInstNullTest("NullYAPutLongFieldTest")
    yaInstNullTest("NullYAPutFloatFieldTest")
    yaInstNullTest("NullYAPutDoubleFieldTest")
    yaInstNullTest("NullYAPutObjectFieldTest")
    
    yaStaticSimpleOutTest("YAGetBooleanStaticTest1","true")
    yaStaticSimpleOutTest("YAGetBooleanStaticTest2","false")
    yaStaticSimpleOutTest("YAGetByteStaticTest","23")
    yaStaticSimpleOutTest("YAGetCharStaticTest1","100")
    yaStaticSimpleOutTest("YAGetCharStaticTest2","50000")
    yaStaticSimpleOutTest("YAGetShortStaticTest1","100")
    yaStaticSimpleOutTest("YAGetShortStaticTest2","-10000")
    yaStaticSimpleOutTest("YAGetIntStaticTest","1000000")
    yaStaticSimpleOutTest("YAGetLongStaticTest","-1000000000000")
    yaStaticSimpleOutTest("YAGetFloatStaticTest","5.525")
    yaStaticSimpleOutTest("YAGetDoubleStaticTest","10.41247642")
    yaStaticSimpleOutTest("YAGetObjectStaticTest","bar")
    
    2.times {
      | idx |
      t=Test.new("InstMethodCalls test#{idx}")
      t.successLine("42 10000 -20000000 5000")
      t.successLine("100000 true false true")
      t.successLine("4203216 65321.25 214.63 -124.64")
      t.successLine("364215 42164327 1.5 -2.5 3634264")
      t.successLine("53215342 3.14159 356342 -125642 -2.7365")
      t.successLine("64264 6432146 4000000000000 -2000000000000 532642")
      t.successLine("-4164231 1222222222222 -3216 531263 -2111111111111")
      t.successLine("-4221 123456789012 364264.64316 53262 32 -5666222888999 -34264.642642")
      t.successLine("-6432 false -10 40000 -20000 5634264")
      t.successLine("-3264214 1012012012012 true 15 45000 -10000 -25421.4264 false -20 35900 10000")
      t.successLine("that seemed to work.")
      t.command("#{@vm} com.fiji.fivm.test.InstMethodCalls 42 10000 -20000000 5000 100000 true false true 4203216 65321.251 214.63 -124.64 364215 42164327 1.5 -2.5 3634264 53215342 3.14159 356342 -125642 -2.7365 64264 6432146 4000000000000 -2000000000000 532642 -4164231 1222222222222 -3216 531263 -2111111111111 -4221 123456789012 364264.64316 53262 32 -5666222888999 -34264.642642 -6432 false -10 40000 -20000 5634264 -3264214 1012012012012 true 15 45000 -10000 -25421.4264 false -20 35900 10000")
      runFivmtestTest(t)
    }
    
    5.times {
      | idx |
      t=Test.new("InterfaceCollisionTest2 test#{idx}")
      t.successPattern("First allocation: com.fiji.fivm.test.InterfaceCollisionTest2$C01")
      t.successPattern("Second allocation: com.fiji.fivm.test.InterfaceCollisionTest2$C02")
      t.successPattern("Third allocation: com.fiji.fivm.test.InterfaceCollisionTest2$C03")
      t.successPattern("Fourth allocation: com.fiji.fivm.test.InterfaceCollisionTest2$C04")
      t.successLine("(1) Assertions all succeeded.")
      t.successLine("(2) Assertions all succeeded.")
      (1..9).each {
        | x |
        t.successLine("C01.foo%02d called"%[x])
        t.successLine("C03.foo%02d called"%[x])
        t.successLine("C03.foo%02d called"%[x+10])
      }
      (2..10).each {
        | x |
        t.successLine("C02.foo%02d called"%[x])
        t.successLine("C04.foo%02d called"%[x])
        t.successLine("C04.foo%02d called"%[x+10])
      }
      t.successLine("Done!")
      t.command("#{@vm} com.fiji.fivm.test.InterfaceCollisionTest2")
      runFivmtestTest(t)
    }
    
    5.times {
      | idx |
      t=Test.new("InterfaceCollisionTest3 test#{idx}")
      2.times {
        | jdx |
        t.successPattern("Allocation #{jdx}: com.fiji.fivm.test.InterfaceCollisionTest3$C#{jdx}")
      }
      t.successLine("Assertions all succeeded.")
      2.times {
        | jdx |
        100.times {
          | kdx |
          t.successLine("C#{jdx}.foo#{jdx*50+kdx} called")
        }
      }
      t.successLine("Done!")
      t.command("#{@vm} com.fiji.fivm.test.InterfaceCollisionTest3")
      runFivmtestTest(t)
    }
    
    t=Test.new("SimpleGCTest #{heapSize(800)}m heap")
    t.successLine("That seems to have worked.")
    t.command("#{@vm} com.fiji.fivm.test.SimpleGCTestMain 10 10000000 10")
    t.env["FIVMR_GC_MAX_MEM"]="#{heapSize(800)}m"
    runFivmtestTest(t)
    
    t=Test.new("SimpleGCTest #{heapSize(250)}m heap")
    t.successLine("That seems to have worked.")
    t.command("#{@vm} com.fiji.fivm.test.SimpleGCTestMain 10 10000000 10")
    t.env["FIVMR_GC_MAX_MEM"]="#{heapSize(250)}m"
    runFivmtestTest(t)
    
    tableSwitchTest("TableSwitchTest1",0,9)
    tableSwitchTest("TableSwitchTest2",0,0)
    tableSwitchTest("TableSwitchTest3",0,9)
    tableSwitchTest("TableSwitchTest4",-5,4)
    tableSwitchTest("TableSwitchTest5",-15,-6)
    tableSwitchTest("TableSwitchTest6",5,14)
    
    simpleInOutTest("LookupSwitchTest1",-1,2000)
    simpleInOutTest("LookupSwitchTest1",0,1000)
    simpleInOutTest("LookupSwitchTest1",1,1100)
    simpleInOutTest("LookupSwitchTest1",2,1200)
    simpleInOutTest("LookupSwitchTest1",3,1300)
    simpleInOutTest("LookupSwitchTest1",4,1400)
    simpleInOutTest("LookupSwitchTest1",5,1500)
    simpleInOutTest("LookupSwitchTest1",6,1600)
    simpleInOutTest("LookupSwitchTest1",7,1700)
    simpleInOutTest("LookupSwitchTest1",8,1800)
    simpleInOutTest("LookupSwitchTest1",9,1900)
    simpleInOutTest("LookupSwitchTest1",10,2000)
    
    simpleInOutTest("LookupSwitchTest2",-5001,2000)
    simpleInOutTest("LookupSwitchTest2",-5000,1000)
    simpleInOutTest("LookupSwitchTest2",-4999,2000)
    simpleInOutTest("LookupSwitchTest2",-16,2000)
    simpleInOutTest("LookupSwitchTest2",-15,1100)
    simpleInOutTest("LookupSwitchTest2",-14,2000)
    simpleInOutTest("LookupSwitchTest2",0,2000)
    simpleInOutTest("LookupSwitchTest2",1,1200)
    simpleInOutTest("LookupSwitchTest2",2,2000)
    simpleInOutTest("LookupSwitchTest2",51,2000)
    simpleInOutTest("LookupSwitchTest2",52,1300)
    simpleInOutTest("LookupSwitchTest2",53,2000)
    simpleInOutTest("LookupSwitchTest2",99,2000)
    simpleInOutTest("LookupSwitchTest2",100,1400)
    simpleInOutTest("LookupSwitchTest2",101,2000)
    simpleInOutTest("LookupSwitchTest2",252,2000)
    simpleInOutTest("LookupSwitchTest2",253,1500)
    simpleInOutTest("LookupSwitchTest2",254,1600)
    simpleInOutTest("LookupSwitchTest2",255,1700)
    simpleInOutTest("LookupSwitchTest2",256,2000)
    simpleInOutTest("LookupSwitchTest2",599,2000)
    simpleInOutTest("LookupSwitchTest2",600,1800)
    simpleInOutTest("LookupSwitchTest2",601,2000)
    simpleInOutTest("LookupSwitchTest2",9999,2000)
    simpleInOutTest("LookupSwitchTest2",10000,1900)
    simpleInOutTest("LookupSwitchTest2",10001,2000)
    
    t=Test.new("MultiANewArray2Dim 1 2 test")
    t.successLine("class [[I class [I")
    t.successLine("1 2")
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray2Dim 1 2")
    runFivmtestTest(t)

    t=Test.new("MultiANewArray2Dim 50 99 test")
    t.successLine("class [[I class [I")
    t.successLine("50 99")
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray2Dim 50 99")
    runFivmtestTest(t)

    t=Test.new("MultiANewArray3Dim 1 2 3 test")
    t.successLine("class [[[F class [[F class [F")
    t.successLine("1 2 3")
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray3Dim 1 2 3")
    runFivmtestTest(t)

    t=Test.new("MultiANewArray3Dim 50 99 71 test")
    t.successLine("class [[[F class [[F class [F")
    t.successLine("50 99 71")
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray3Dim 50 99 71")
    runFivmtestTest(t)
    
    t=Test.new("MultiANewArray7Dim 1 2 3 4 5 6 7 test")
    t.successLine("class [[[[[[[B class [[[[[[B class [[[[[B class [[[[B class [[[B class [[B class [B")
    t.successLine("1 2 3 4 5 6 7")
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray7Dim 1 2 3 4 5 6 7")
    runFivmtestTest(t)
    
    numlist=""
    100.times {
      | idx |
      if idx!=0
        numlist+=" "
      end
      if idx==50
        numlist+="2"
      else
        numlist+="1"
      end
    }
    
    classlist=""
    100.times {
      | idx |
      if idx!=0
        classlist+=" "
      end
      classlist+="class "
      (100-idx).times {
        classlist+="["
      }
      classlist+="B"
    }
    
    t=Test.new("MultiANewArray100Dim "+numlist+" test")
    t.successLine(classlist)
    t.successLine(numlist)
    t.command("#{@vm} com.fiji.fivm.test.MultiANewArray100Dim #{numlist}")
    runFivmtestTest(t)
    
    5.times {
      | idx |
      t=Test.new("SyncInstMethodTest1 test#{idx}")
      t.successLine("That worked.")
      t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodTest1")
      runFivmtestTest(t)
    }
    
    t=Test.new("SyncInstMethodRetIntTest test")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetIntTest")
    runFivmtestTest(t)
    
    t=Test.new("SyncInstMethodRetBoolTest test")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetBoolTest")
    runFivmtestTest(t)
    
    t=Test.new("SyncInstMethodRetObjTest test")
    t.successLine("That worked.")
    10.times {
      | idx |
      t.successLine("foo bar #{idx*1000000} baz")
    }
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetObjTest")
    runFivmtestTest(t)
    
    t=Test.new("SyncInstMethodRetDoubleTest test")
    t.successLine("That worked.")
    t.successLine("-5.0000049999985E12")
    t.successLine("-4.0000039999985E12")
    t.successLine("-3.0000029999985E12")
    t.successLine("-2.0000019999985E12")
    t.successLine("-1.0000009999985E12")
    t.successLine("1.5")
    t.successLine("1.0000010000015E12")
    t.successLine("2.0000020000015E12")
    t.successLine("3.0000030000015E12")
    t.successLine("4.0000040000015E12")
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetDoubleTest")
    runFivmtestTest(t)
    
    t=Test.new("SyncInstMethodRetFloatTest test")
    t.successLine("That worked.")
    10.times {
      | idx |
      t.successLine("#{-9999999+idx*2000000}.0")
    }
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetFloatTest")
    runFivmtestTest(t)
    
    t=Test.new("SyncInstMethodRetLongTest test")
    t.successLine("That worked.")
    10.times {
      | idx |
      t.successLine("#{-50000005000000+idx*10000001000000}")
    }
    t.command("#{@vm} com.fiji.fivm.test.SyncInstMethodRetLongTest")
    runFivmtestTest(t)
    
    t=Test.new("BadInterfaceCall test")
    t.successLine("java.lang.NullPointerException")
    t.command("#{@vm} com.fiji.fivm.test.BadInterfaceCall",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("BadInterfaceCall2 test")
    t.successLine("java.lang.IncompatibleClassChangeError")
    t.command("#{@vm} com.fiji.fivm.test.BadInterfaceCall2",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("HashMapCallback 1 2 3 4 5 6 7 8 test")
    t.successLine("0: 1 2")
    t.successLine("2: 3 4")
    t.successLine("4: 5 6")
    t.successLine("6: 7 8")
    t.successLine("done.")
    t.command("#{@vm} com.fiji.fivm.test.HashMapCallback 1 2 3 4 5 6 7 8")
    runFivmtestTest(t)
    
    t=Test.new("HashMapCallback 36 12 -90 100 8356 -1 test")
    t.successLine("0: 36 12")
    t.successLine("2: -90 100")
    t.successLine("4: 8356 -1")
    t.successLine("done.")
    t.command("#{@vm} com.fiji.fivm.test.HashMapCallback 36 12 -90 100 8356 -1")
    runFivmtestTest(t)
    
    t=Test.new("TryCatchTest1 test")
    t.successLine("caught com.fiji.fivm.test.TryCatchTest1$E")
    t.successLine("foo = bar")
    t.successLine("done.")
    t.failPattern("badness")
    t.command("#{@vm} com.fiji.fivm.test.TryCatchTest1")
    runFivmtestTest(t)
    
    t=Test.new("TryCatchTest2 test")
    t.successLine("caught com.fiji.fivm.test.TryCatchTest2$E")
    t.successLine("foo = bar")
    t.successLine("done.")
    t.failPattern("badness")
    t.command("#{@vm} com.fiji.fivm.test.TryCatchTest2")
    runFivmtestTest(t)
    
    t=Test.new("CloneTest1 test")
    t.successPattern("o1 = com.fiji.fivm.test.CloneTest1$C")
    t.successPattern("o2 = com.fiji.fivm.test.CloneTest1$C")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.CloneTest1")
    runFivmtestTest(t)
    
    t=Test.new("CloneTest2 100000 test")
    t.successPattern("o1 = com.fiji.fivm.test.CloneTest2$C")
    t.successPattern("o2 = com.fiji.fivm.test.CloneTest2$C")
    t.successLine("o1.x = 100000")
    t.successLine("o2.x = 100000")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.CloneTest2 100000")
    runFivmtestTest(t)
    
    t=Test.new("CloneTest3 1 2 3 4 5 6 7 test")
    t.successPattern("o1 = com.fiji.fivm.test.CloneTest3$C")
    t.successPattern("o2 = com.fiji.fivm.test.CloneTest3$C")
    t.successLine("o1.a = 1")
    t.successLine("o1.b = 2")
    t.successLine("o1.c = 3")
    t.successLine("o1.d = 4")
    t.successLine("o1.e = 5")
    t.successLine("o1.f = 6")
    t.successLine("o1.g = 7")
    t.successLine("o2.a = 1")
    t.successLine("o2.b = 2")
    t.successLine("o2.c = 3")
    t.successLine("o2.d = 4")
    t.successLine("o2.e = 5")
    t.successLine("o2.f = 6")
    t.successLine("o2.g = 7")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.CloneTest3 1 2 3 4 5 6 7")
    runFivmtestTest(t)
    
    t=Test.new("CloneTest4 1 2 3 foo 5 6 7 test")
    t.successPattern("o1 = com.fiji.fivm.test.CloneTest4$C")
    t.successPattern("o2 = com.fiji.fivm.test.CloneTest4$C")
    t.successLine("o1.a = 1")
    t.successLine("o1.b = 2")
    t.successLine("o1.c = 3")
    t.successLine("o1.d = foo")
    t.successLine("o1.e = 5")
    t.successLine("o1.f = 6")
    t.successLine("o1.g = 7")
    t.successLine("o2.a = 1")
    t.successLine("o2.b = 2")
    t.successLine("o2.c = 3")
    t.successLine("o2.d = foo")
    t.successLine("o2.e = 5")
    t.successLine("o2.f = 6")
    t.successLine("o2.g = 7")
    t.successLine("That worked.")
    t.command("#{@vm} com.fiji.fivm.test.CloneTest4 1 2 3 foo 5 6 7")
    runFivmtestTest(t)
    
    simpleInOutTest("BooleanArrayToString",
                    "true false false true true false true true false false false true false true false true true false false false true false true true true",
                    "[true, false, false, true, true, false, true, true, false, false, false, true, false, true, false, true, true, false, false, false, true, false, true, true, true]")
    simpleInOutTest("ByteArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("ByteArrayCopy",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("ShortArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("IntArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("IntArrayCopy",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("LongArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("LongArrayCopy",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1, 75, 52, 123, -12, 54, -36, 87, -6, 36, 73, -32, 20, -93, 49, -92, 92, -85, -82, 29, -62, -95, 17]")
    simpleInOutTest("FloatArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1.0, 75.0, 52.0, 123.0, -12.0, 54.0, -36.0, 87.0, -6.0, 36.0, 73.0, -32.0, 20.0, -93.0, 49.0, -92.0, 92.0, -85.0, -82.0, 29.0, -62.0, -95.0, 17.0]")
    simpleInOutTest("DoubleArrayToString",
                    "1 75 52 123 -12 54 -36 87 -6 36 73 -32 20 -93 49 -92 92 -85 -82 29 -62 -95 17",
                    "[1.0, 75.0, 52.0, 123.0, -12.0, 54.0, -36.0, 87.0, -6.0, 36.0, 73.0, -32.0, 20.0, -93.0, 49.0, -92.0, 92.0, -85.0, -82.0, 29.0, -62.0, -95.0, 17.0]")
    simpleInOutTest("CharArrayToString",
                    "h e l l o w o r l d ! t h i s s h o u l d a p p e a r w i t h o u t s p a c e s",
                    "[h, e, l, l, o, w, o, r, l, d, !, t, h, i, s, s, h, o, u, l, d, a, p, p, e, a, r, w, i, t, h, o, u, t, s, p, a, c, e, s]")
    simpleInOutTest("ObjectArrayToString",
                    "h e l l o w o r l d ! t h i s s h o u l d a p p e a r w i t h o u t s p a c e s",
                    "[[0] = h, [1] = e, [2] = l, [3] = l, [4] = o, [5] = w, [6] = o, [7] = r, [8] = l, [9] = d, [10] = !, [11] = t, [12] = h, [13] = i, [14] = s, [15] = s, [16] = h, [17] = o, [18] = u, [19] = l, [20] = d, [21] = a, [22] = p, [23] = p, [24] = e, [25] = a, [26] = r, [27] = w, [28] = i, [29] = t, [30] = h, [31] = o, [32] = u, [33] = t, [34] = s, [35] = p, [36] = a, [37] = c, [38] = e, [39] = s]")
    simpleInOutTest("ObjectArrayCopy",
                    "h e l l o w o r l d ! t h i s s h o u l d a p p e a r w i t h o u t s p a c e s",
                    "[[0] = h, [1] = e, [2] = l, [3] = l, [4] = o, [5] = w, [6] = o, [7] = r, [8] = l, [9] = d, [10] = !, [11] = t, [12] = h, [13] = i, [14] = s, [15] = s, [16] = h, [17] = o, [18] = u, [19] = l, [20] = d, [21] = a, [22] = p, [23] = p, [24] = e, [25] = a, [26] = r, [27] = w, [28] = i, [29] = t, [30] = h, [31] = o, [32] = u, [33] = t, [34] = s, [35] = p, [36] = a, [37] = c, [38] = e, [39] = s]")
    
    simpleNullTest("NullArrayLength")
    simpleNullTest("NullThrow")
    
    ['Boolean','Byte','Char','Short','Int','Long','Float','Double','Object'].each {
      | type |
      ['Store','Load'].each {
        | opr |
        [-1,0,1].each {
          | idx |
          simpleNullTest("Null#{type}Array#{opr} #{idx}")
        }
      }
    }
    
    t=Test.new("BadInit test")
    t.successPattern("ExceptionInInitializerError")
    t.successPattern("RuntimeException: ha!  I'm an exception!")
    t.command("#{@vm} com.fiji.fivm.test.BadInit",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("TestBadInit test")
    t.successLine("Caught an EIIE.  That's good.")
    t.successPattern("ExceptionInInitializerError")
    t.successPattern("RuntimeException: ha!  I'm an exception!")
    t.successLine("Caught a NCDFE.  That's good.")
    t.successPattern("NoClassDefFoundError")
    t.successLine("Success!")
    t.command("#{@vm} com.fiji.fivm.test.TestBadInit")
    runFivmtestTest(t)
    
    t=Test.new("TestBadInit2 test")
    t.successLine("Caught an EIIE.  That's good.")
    t.successPattern("ExceptionInInitializerError")
    t.successPattern("RuntimeException: ha!  I'm an exception!")
    t.successLine("Caught a NCDFE.  That's good.")
    t.successPattern("NoClassDefFoundError")
    t.successLine("Success!")
    t.command("#{@vm} com.fiji.fivm.test.TestBadInit")
    runFivmtestTest(t)
    
    t=Test.new("BadFieldAccessTest test")
    t.successPattern("Iteration 0: java.lang.NoSuchFieldError")
    t.successPattern("Iteration 1: java.lang.NoSuchFieldError")
    t.command("#{@vm} com.fiji.fivm.test.BadFieldAccessTest")
    runFivmtestTest(t)
    
    t=Test.new("BadFieldAccess2Test test")
    t.successPattern("Iteration 0: java.lang.NoClassDefFoundError")
    t.successPattern("Iteration 1: java.lang.NoClassDefFoundError")
    t.command("#{@vm} com.fiji.fivm.test.BadFieldAccess2Test")
    runFivmtestTest(t)
    
    t=Test.new("StressIntArrayAlloc 100 10000 1000 test")
    t.successLine("Success.")
    t.command("#{@vm} com.fiji.fivm.test.StressIntArrayAlloc 100 10000 1000")
    runFivmtestTest(t)
    
    t=Test.new("StressIntArrayAlloc 10 10000 1000 test")
    t.successLine("Success.")
    t.command("#{@vm} com.fiji.fivm.test.StressIntArrayAlloc 10 10000 1000")
    runFivmtestTest(t)
    
    t=Test.new("StressIntArrayAlloc 1 10000 1000 test")
    t.successLine("Success.")
    t.command("#{@vm} com.fiji.fivm.test.StressIntArrayAlloc 1 10000 1000")
    runFivmtestTest(t)

    arrayTests2("Foo") { |x| "Foo[#{x}]" }
    arrayTests2("Foo","abc") { |x| "Foo[#{x}]" }
    arrayTests2("Foo","bca") { |x| "Foo[#{x}]" }
    arrayTests2("Foo","foo","bar") { |x| "Foo[#{x}]" }
    arrayTests2("Foo","blah","bleh","blih") { |x| "Foo[#{x}]" }
    arrayTests2("Foo","gsdajlgsa","grbwhigfhgsa","lkjghjlgsa") { |x| "Foo[#{x}]" }
    
    t=Test.new("EvilClinit test")
    t.successLine("I ran, x = 42")
    t.successLine("x should be = 42")
    t.command("#{@vm} com.fiji.fivm.test.EvilClinit")
    runFivmtestTest(t)
    
    t=Test.new("EvilClinit2 test")
    t.successPattern("Main method caught java.lang.ExceptionInInitializerError")
    t.successPattern("Thread caught java.lang.NoClassDefFoundError")
    t.command("#{@vm} com.fiji.fivm.test.EvilClinit2")
    runFivmtestTest(t)
    
    t=Test.new("EvilClinit3 test")
    # we actually don't know which of these will trigger first...
    #t.successPattern("evil.x returned successfully")
    #t.successPattern("run() ran successfully")
    t.command("#{@vm} com.fiji.fivm.test.EvilClinit3",
              :exitStatus=>1)
    runFivmtestTest(t)
    
    t=Test.new("CLTest test")
    t.successLine("foo")
    t.successLine("that worked!")
    t.successPattern("bar class com.fiji.fivm.test.CLTest$Bar com.fiji.fivm.r1.AppClassLoader")
    t.successPattern("bar class com.fiji.fivm.test.CLTest$Bar com.fiji.fivm.test.CLTest$1")
    t.command("#{@vm} com.fiji.fivm.test.CLTest")
    runFivmtestTest(t)
    
    t=Test.new("JniTest test")
    t.successLine("Calling simpleTest()...")
    t.successLine("simpleTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("Calling simpleRetTest()...")
    t.successLine("simpleRetTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("Calling simpleSyncTest()...")
    t.successLine("simpleSyncTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("Calling simpleSyncRetTest()...")
    t.successLine("simpleSyncRetTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("Calling simpleThrowTest()...")
    t.successLine("simpleThrowTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("com.fiji.fivm.test.JniTest$MyException: Hi, I'm an exception, thrown from native code!");
    t.successLine("   at com.fiji.fivm.test.JniTest.simpleThrowTest(Native Method)")
    t.successLine("Calling simpleSyncThrowTest()...")
    t.successLine("simpleSyncThrowTest() called from com.fiji.fivm.test.JniTest");
    t.successLine("com.fiji.fivm.test.JniTest$MyException: Hi, I'm an exception, thrown from native synchronized code!");
    t.successLine("   at com.fiji.fivm.test.JniTest.simpleSyncThrowTest(Native Method)")
    t.successLine("That worked!")
    t.env["JAVA_JNI_PATH"]="test/c/jni"
    t.command("#{@vm} com.fiji.fivm.test.JniTest")
    runFivmtestTest(t)
    
    [[4,42],[-100000,2795],[0,-125531]].each {
      | args |
      t=Test.new("JniTest2 #{args[0]} #{args[1]} test")
      t.successLine("Calling twoInts()...")
      t.successLine("twoInts() called from com.fiji.fivm.test.JniTest2")
      t.successLine("a = #{args[0]}, b = #{args[1]}")
      t.successLine("That worked!")
      t.env["JAVA_JNI_PATH"]="test/c/jni"
      t.command("#{@vm} com.fiji.fivm.test.JniTest2 #{args[0]} #{args[1]}")
      runFivmtestTest(t)
    }
    
    [[-265,231],[-100000,2795],[-530264267,-125531]].each {
      | args |
      t=Test.new("JniTest3 #{args[0]} #{args[1]} test")
      t.successLine("Calling twoInts()...")
      t.successLine("twoInts() called from Instance #1 of JniTest3")
      t.successLine("a = #{args[0]}, b = #{args[1]}, me = 1")
      t.successLine("Got back: #{1+args[0]+args[1]}")
      t.successLine("That worked!")
      t.env["JAVA_JNI_PATH"]="test/c/jni"
      t.command("#{@vm} com.fiji.fivm.test.JniTest3 #{args[0]} #{args[1]}")
      runFivmtestTest(t)
    }
    
    t=Test.new("StackOverflow test")
    t.successLine("java.lang.StackOverflowError")
    t.successLine("That worked!")
    t.command("#{@vm} com.fiji.fivm.test.StackOverflow")
    runFivmtestTest(t)
    
    t=Test.new("MTGCTest2 test")
    t.successLine("We didn't crash.  Success.")
    t.env["FIVMR_GC_MAX_MEM"]="1g"
    t.command("#{@vm} com.fiji.fivm.test.MTGCTest2")
    runFivmtestTest(t)
    
    [100000, 100, 1].each {
      | cnt |
      t=Test.new("ManyFields #{cnt} test")
      t.successLine("That worked!")
      t.command("#{@vm} com.fiji.fivm.test.ManyFields #{cnt}")
      runFivmtestTest(t)
    }
    
    t=Test.new("Finalize 10000000 test")
    t.successLine("I'm finalized!")
    t.successLine("That worked!")
    t.command("#{@vm} com.fiji.fivm.test.Finalize 10000000")
    runFivmtestTest(t)
    
    t=Test.new("Finalize2 10000000 test")
    t.successLine("#2: cnt = 10000000")
    t.successLine("That worked!")
    t.command("#{@vm} com.fiji.fivm.test.Finalize2 10000000")
    runFivmtestTest(t)
    
    t=Test.new("PrintSysProps test")
    t.successLine("java.vm.name=fivm")
    t.successLine("#PrintSysProps")
    t.successLine("java.vm.vendor=Fiji Systems Inc.")
    t.command("#{@vm} com.fiji.fivm.test.PrintSysProps")
    runFivmtestTest(t)
    
    t=Test.new("PrintSysProps -Dthis=\"that foo bar\" test")
    t.successLine("java.vm.name=fivm")
    t.successLine("#PrintSysProps")
    t.successLine("java.vm.vendor=Fiji Systems Inc.")
    t.successLine("this=that foo bar")
    t.env["FIVMR_SYS_PROPS"]="{this=\"that foo bar\"}"
    t.command("#{@vm} com.fiji.fivm.test.PrintSysProps")
    runFivmtestTest(t)
    
    t=Test.new("EvilClinit4 test")
    t.successLine("Evil created.")
    t.successLine("e.y = 65, evilInited = false")
    t.successLine("Evil initialized.")
    t.successLine("evil.x = 42")
    t.command("#{@vm} com.fiji.fivm.test.EvilClinit4")
    runFivmtestTest(t)
  end
end

CLTests.new(ARGV[0],ARGV[1].to_f).run

