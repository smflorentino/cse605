#!/usr/bin/env ruby

require 'pathname'

$home=Pathname.new($0).realpath.parent.parent.parent

require ($home+'lib'+'fijiconfig.rb')

def testMap1
  conf={'foo'=>'bar'}
  conf2=FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==conf2
  puts FijiConfig::dump(conf2)
end

def testMap2
  conf={
    'foo bar'=>'42',
    'this'=>'that',
    'awesome'=>'true',
    'something'=>'something else',
    'stuff'=>"Just some random text, thanks.\n"
  }
  puts FijiConfig::dump(conf)
  puts FijiConfig::dumpPretty(conf)
  conf2=FijiConfig::parse(FijiConfig::dump(conf))
  puts FijiConfig::dump(conf2)
  raise unless conf==conf2
  conf3=FijiConfig::parse(FijiConfig::dumpPretty(conf))
  puts FijiConfig::dumpPretty(conf3)
  raise unless conf==conf3
  raise unless conf2==conf3
end

def testParse1
  raise unless FijiConfig::parse("\"\\N\\R\\T\"")=="\n\r\t"
  raise unless FijiConfig::parse("\"\\n\\r\\t\"")=="\n\r\t"
end

def testParse2
  conf=FijiConfig.parse("{foo=bar; this=that}")
  raise unless conf=={"foo"=>"bar", "this"=>"that"}
  puts FijiConfig::dump(conf)
  puts FijiConfig::dumpPretty(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testParse3
  conf=FijiConfig.parse("(foo, bar, baz)")
  raise unless conf==["foo","bar","baz"]
  puts FijiConfig::dump(conf)
  puts FijiConfig::dumpPretty(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testParse4
  conf=FijiConfig.parse("{ message = \"\\150\\x65llo, wor\\154\\x64\\041\" }")
  raise unless conf=={"message"=>"hello, world!"}
  puts FijiConfig.dump(conf)
  puts FijiConfig::dumpPretty(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList1
  conf=[]
  10.times {
    | i |
    conf << i.to_s
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList2
  conf=[]
  30.times {
    | i |
    conf << i.to_s
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList3
  conf=[]
  100.times {
    | i |
    conf << i.to_s
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList4
  conf=[]
  50.times {
    | i |
    conf2=[]
    20.times {
      | j |
      conf2 << (i+j).to_s
    }
    conf << conf2
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList5
  conf=[]
  50.times {
    | i |
    conf2=[]
    5.times {
      | j |
      conf2 << (i+j).to_s
    }
    conf << conf2
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList6
  conf=[]
  50.times {
    | i |
    conf2=[]
    2.times {
      | j |
      conf2 << (i+j).to_s
    }
    conf << conf2
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList7
  conf=[]
  50.times {
    | i |
    conf << {"foo"=>"bar"}
  }
  puts FijiConfig::dumpPretty(conf)
  puts FijiConfig::dump(conf)
  raise unless conf==FijiConfig::parse(FijiConfig::dump(conf))
  raise unless conf==FijiConfig::parse(FijiConfig::dumpPretty(conf))
end

def testList8
  raise unless FijiConfig.parse("()")==[]
  raise unless FijiConfig.dump([])=="()"
  raise unless FijiConfig.dumpPrettyWithMsg([],"")=="# #{$/}()"
end

def testAtom1
  raise unless FijiConfig::dump("")=='""'
  puts FijiConfig::dumpPrettyWithMsg("","")
  raise unless FijiConfig::dumpPrettyWithMsg("","")=="# #{$/}\"\""
end

def testAtom2
  puts FijiConfig::parse("foo # foo")
  raise unless FijiConfig::parse("foo # foo")=="foo"
end

def testLineLiteral1
  puts FijiConfig::parse("!")
  raise unless FijiConfig::parse("!")==""
end

def testLineLiteral2
  puts FijiConfig::parse("!fooo bar# {} () !gdshj")
  raise unless FijiConfig::parse("!fooo bar# {} () !gdshj")=="fooo bar# {} () !gdshj"
end

def testLineLiteral3
  puts FijiConfig::parse("!fooo bar# {} () !gdshj\r     # foo bar")
  raise unless FijiConfig::parse("!fooo bar# {} () !gdshj\r     # foo bar")=="fooo bar# {} () !gdshj"
end

def testLineLiteral4
  map=FijiConfig::parse("{ stuff = !fooo bar# {} () !gdshj\r   otherStuff = bar }")
  puts FijiConfig::dump(map)
  raise unless map['stuff']=="fooo bar# {} () !gdshj"
  raise unless map['otherStuff']=="bar"
end

def testLineLiteral5
  map=FijiConfig::parse("{ stuff = !fooo \"bar\"# {} () !gdshj\r   otherStuff = bar }")
  puts FijiConfig::dump(map)
  raise unless map['stuff']=="fooo \"bar\"# {} () !gdshj"
  raise unless map['otherStuff']=="bar"
end

def testLineLiteral6
  map=FijiConfig::parse("{ stuff = !fooo \"bar\"# {} () !gdshj\n   otherStuff = bar }")
  puts FijiConfig::dump(map)
  raise unless map['stuff']=="fooo \"bar\"# {} () !gdshj"
  raise unless map['otherStuff']=="bar"
end

def testLineLiteral7
  list=FijiConfig::parse("( !foo\n!bar\n!baz\n )")
  puts FijiConfig::dump(list)
  raise unless list==["foo","bar","baz"]
end

def testMultiLineLiteral1
  atom=FijiConfig::parse("<<EOF\nfoo bar stuff\nyup this should workEOF # ignored")
  puts FijiConfig::dump(atom)
  raise unless atom=="foo bar stuff\nyup this should work"
end

def testMultiLineLiteral2
  atom=FijiConfig::parse("<<EOF\r\nfoo bar stuff\nyup this should workEOF # ignored")
  puts FijiConfig::dump(atom)
  raise unless atom=="foo bar stuff\nyup this should work"
end

def testMultiLineLiteral3
  atom=FijiConfig::parse("<<EOF\n\rfoo bar stuff\nyup this should workEOF # ignored")
  puts FijiConfig::dump(atom)
  raise unless atom=="foo bar stuff\nyup this should work"
end

def testMultiLineLiteral4
  atom=FijiConfig::parse("<<EOF\rfoo bar stuff\nyup this should workEOF # ignored")
  puts FijiConfig::dump(atom)
  raise unless atom=="foo bar stuff\nyup this should work"
end

testMap1
testMap2
testParse1
testParse2
testParse3
testParse4
testList1
testList2
testList3
testList4
testList5
testList6
testList7
testList8
testAtom1
testAtom2
testLineLiteral1
testLineLiteral2
testLineLiteral3
testLineLiteral4
testLineLiteral5
testLineLiteral6
testLineLiteral7
testMultiLineLiteral1
testMultiLineLiteral2
testMultiLineLiteral3
testMultiLineLiteral4

