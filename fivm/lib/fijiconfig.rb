# This code is public domain.

module FijiConfig
  
  class Util
    def self.isalnum(c)
      ((c>=?a and c<=?z) or
       (c>=?A and c<=?Z) or
       (c>=?0 and c<=?9))
    end
    
    def self.isxdigit(c)
      ((c>=?a and c<=?f) or
       (c>=?A and c<=?F) or
       (c>=?0 and c<=?9))
    end
    
    def self.isodigit(c)
      (c>=?0 and c<=?7)
    end
    
    def self.fromHex(c)
      if c>=?0 and c<=?9
        c-?0
      elsif c>=?a and c<=?f
        c-?a
      else
        c-?A
      end
    end
    
    def self.fromOct(c)
      c-?0
    end
    
    def self.isIdentifier(c)
      case c
      when ?_, ?$, ?-, ?/, ?., ?+
        true
      else
        isalnum(c)
      end
    end
   
    if RUBY_VERSION[0..1] == "2." or RUBY_VERSION[0..2] == "1.9"
      def self.confIsIdentifier(conf)
        if conf.empty?
          false
        else
          conf.each_char {
            | c |
            return false unless isIdentifier(c)
          }
          true
        end
      end
    else
      def self.confIsIdentifier(conf)
        if conf.empty?
          false
        else
          conf.each_byte {
            | c |
            return false unless isIdentifier(c)
          }
          true
        end
      end
    end
    
    def self.dumpStr(conf)
      raise if conf==nil
      if conf==true
        "yes"
      elsif conf==false
        "no"
      else
        confStr=conf.to_s
        if confIsIdentifier(confStr)
          confStr
        else
          confStr.inspect
        end
      end
    end
    
    def self.dumpShort(conf)
      str=''
      if conf.is_a? Hash
        str << '{'
        conf.each_pair {
          | key, value |
          str << dumpStr(key)
          str << '='
          if value==nil
            raise "Refusing to dump map value #{key} = nil in #{conf.inspect}"
          end
          str << dumpShort(value)
          str << ';'
        }
        str << '}'
      elsif conf.is_a? Array
        str << '('
        first=true
        conf.each {
          | value |
          if first
            first=false
          else
            str << ','
          end
          if value==nil
            raise "Refusing to dump nil list value in #{conf.inspect}"
          end
          str << dumpShort(value)
        }
        str << ')'
      else
        str << dumpStr(conf)
      end
      str
    end
    
    def self.forceMultiLine(conf)
      conf.is_a? Hash and conf.size>1
    end
    
    def self.eachPairSorted(conf)
      conf.keys.sort{|a,b| a.to_s<=>b.to_s}.each {
        | key |
        yield key, conf[key]
      }
    end
    
    def self.dumpPrettyShort(conf)
      str=''
      if conf.is_a? Hash
        if conf.empty?
          str << "{}"
        else
          str << '{ '
          eachPairSorted(conf) {
            | key, value |
            str << dumpStr(key)
            str << ' = '
            if value==nil
              raise "Refusing to dump map value #{key} = nil in #{conf.inspect}"
            end
            str << dumpPrettyShort(value)
            str << '; '
          }
          str << '}'
        end
      elsif conf.is_a? Array
        if conf.empty?
          str << "()"
        else
          str << '( '
          first=true
          conf.each {
            | value |
            if first
              first=false
            else
              str << ', '
            end
            if value==nil
              raise "Refusing to dump nil list value in #{conf.inspect}"
            end
            str << dumpPrettyShort(value)
          }
          str << ' )'
        end
      else
        str << dumpStr(conf)
      end
      str
    end
    
    def self.indent(str,indentAmount)
      indentAmount.times {
        str << ' '
      }
    end
    
    def self.dumpPrettyMultiLine(conf,indentLevel,indentStep)
      str=''
      if conf.is_a? Hash
        if conf.empty?
          str << "{}"
        else
          str << '{'
          str << $/
          indent(str,indentLevel)
          eachPairSorted(conf) {
            | key, value |
            indent(str,indentStep)
            str << dumpStr(key)
            str << ' = '
            if value==nil
              raise "Refusing to dump map value #{key} = nil in #{conf.inspect}"
            end
            str << Util::dumpPretty(value,indentLevel+indentStep,indentStep)
            str << ';'
            str << $/
            indent(str,indentLevel)
          }
          str << '}'
        end
      elsif conf.is_a? Array
        if conf.empty?
          str << "()"
        else
          longForm=false
          
          if indentLevel<40
            maxSize=20
          elsif indentLevel<60
            maxSize=10
          else
            maxSize=5
          end
          
          conf.each {
            | sub |
            if dumpPrettyShort(sub).size>maxSize
              longForm=true
              break
            end
          }
          
          if longForm
            str << '('
            str << $/
            indent(str,indentLevel+indentStep)
            first=true
            conf.each {
              | value |
              if first
                first=false
              else
                str << ','
                str << $/
                indent(str,indentLevel+indentStep)
              end
              if value==nil
                raise "Refusing to dump nil list value in #{conf.inspect}"
              end
              str << Util::dumpPretty(value,indentLevel+indentStep,indentStep)
            }
            str << $/
            indent(str,indentLevel)
            str << ')'
          else
            str << '('
            str << $/
            indent(str,indentLevel+indentStep)
            subStr=dumpPrettyShort(conf[0])
            str << subStr
            account=75-indentLevel-indentStep-subStr.size
            conf[1..-1].each {
              | subConf |
              subStr=dumpPrettyShort(subConf)
              if account-subStr.size-2<0
                str << ','
                str << $/
                indent(str,indentLevel+indentStep)
                str << subStr
                account=75-indentLevel-indentStep-subStr.size
              else
                str << ', '
                str << subStr
                account-=2
                account-=subStr.size
              end
            }
            str << $/
            indent(str,indentLevel)
            str << ')'
          end
        end
      else
        str << dumpStr(conf)
      end
      str
    end
    
    def self.dumpPretty(conf,indentLevel,indentStep)
      if forceMultiLine(conf)
        dumpPrettyMultiLine(conf,indentLevel,indentStep)
      else
        result=dumpPrettyShort(conf)
        if result.size+indentLevel > 70
          dumpPrettyMultiLine(conf,indentLevel,indentStep)
        else
          result
        end
      end
    end
  end
  
  class Parser
    def initialize(str,line)
      @str=str
      @idx=0
      @line=line
    end
    
    def parse
      proceedThroughWS
      result=parseNode
      parseWS
      result
    end
    
    def parseNode
      case @str[@idx]
      when ?!
        parseLineLiteralString
      when ?<
        parseMultiLineLiteralString
      when ?"
        parseQuotedString
      when ?(
        parseList
      when ?{
        parseMap
      else
        if Util::isIdentifier(@str[@idx])
          parseIdentifier
        else
          parseError
        end
      end
    end
    
    def parseMap
      assertNotEOF
      if @str[@idx]!=?{
        parseError
      end
      
      proceed
      
      result={}
      
      loop {
        proceedThroughWS
        
        if @str[@idx]==?, or @str[@idx]==?;
          proceed
          next
        end
        
        if @str[@idx]==?}
          @idx+=1
          return result
        end
        
        key=parseString
        
        proceedThroughWS
        
        if @str[@idx]!=?=
          parseError
        end
        
        proceed
        proceedThroughWS
        
        result[key]=parseNode
      }
    end
    
    def parseList
      assertNotEOF
      if @str[@idx]!=?(
        parseError
      end
      
      result=[]
      
      proceed
      
      loop {
        proceedThroughWS
        
        if @str[@idx]==?, or @str[@idx]==?;
          proceed
          next
        end
        
        if @str[@idx]==?)
          @idx+=1
          return result
        end
        
        result << parseNode
      }
    end
    
    def parseString
      assertNotEOF
      
      if @str[@idx]==?"
        parseQuotedString
      elsif @str[@idx]==?!
        parseLineLiteralString
      elsif @str[@idx]==?<
        parseMultiLineLiteralString
      elsif Util::isIdentifier(@str[@idx])
        parseIdentifier
      else
        parseError
      end
    end
    
    def parseQuotedString
      assertNotEOF
      if @str[@idx]!=?"
        parseError
      end
      
      proceed
      
      result=''
      
      loop {
        cont=true
        case @str[@idx]
        when ?"
          cont=false
        when ?\\ then
          proceed
          case @str[@idx]
          when ?n, ?N
            result << "\n"
          when ?r, ?R
            result << "\r"
          when ?t, ?T
            result << "\t"
          when ?x, ?X
            proceed
            hexhi=@str[@idx]
            proceed
            hexlo=@str[@idx]
            
            if (not Util::isxdigit(hexhi) or
                not Util::isxdigit(hexlo))
              parseError
            end
            
            result << ((Util::fromHex(hexlo) << 0)|
                       (Util::fromHex(hexhi) << 4)).chr
          when ?0, ?1, ?2, ?3
            octhi=@str[@idx]
            proceed
            octmd=@str[@idx]
            proceed
            octlo=@str[@idx]
            
            if (not Util::isodigit(octhi) or
                not Util::isodigit(octmd) or
                not Util::isodigit(octlo))
              parseError
            end
            
            result << ((Util::fromOct(octlo) << 0)|
                       (Util::fromOct(octmd) << 3)|
                       (Util::fromOct(octhi) << 6)).chr
          else
            result << @str[@idx]
          end
        else
          result << @str[@idx]
        end
        if cont
          proceed
        else
          break
        end
      }
      
      @idx+=1
      
      result
    end
    
    def parseLineLiteralString
      assertNotEOF
      if @str[@idx]!=?!
        parseError
      end
      
      @idx+=1
      
      result=''
      
      while @idx!=@str.size
        nextChr=@str[@idx]
        if nextChr==?\r or nextChr==?\n
          break
        end
        
        result << nextChr
        @idx+=1
      end
      
      result
    end
    
    def checkTerminator(terminator)
      if @str.size-@idx >= terminator.size
        terminator.size.times {
          | i |
          if @str[@idx+i]!=terminator[i]
            return false
          end
        }
        @idx+=terminator.size
        return true
      else
        return false
      end
    end
    
    def parseMultiLineLiteralString
      assertNotEOF

      if @str[@idx]!=?<
        parseError
      end

      proceed

      if @str[@idx]!=?<
        parseError
      end
      
      proceed

      # figure out the terminator
      terminator=''
      loop {
        nextChr=@str[@idx]
        if nextChr==?\r or nextChr==?\n
          break
        end
        
        terminator << nextChr
        proceed
      }
      
      # get to the next line
      gotToNextLine=false
      unless gotToNextLine
        gotToNextLine=checkTerminator("\r\n")
      end
      unless gotToNextLine
        gotToNextLine=checkTerminator("\n\r")
      end
      unless gotToNextLine
        gotToNextLine=checkTerminator("\n")
      end
      unless gotToNextLine
        gotToNextLine=checkTerminator("\r")
      end
      
      unless gotToNextLine
        parseError
      end
      
      @line+=1
      
      result=''
      
      loop {
        nextChr=@str[@idx]
        if nextChr==?\n
          @line+=1
        end
        if nextChr==terminator[0] and checkTerminator(terminator)
          break
        end
        
        result << nextChr
        proceed
      }
      
      result
    end
    
    def parseIdentifier
      assertNotEOF
      if not Util::isIdentifier(@str[@idx])
        parseError
      end
      
      result=''
      
      while @idx!=@str.size and Util::isIdentifier(@str[@idx])
        result << @str[@idx]
        @idx+=1
      end
      
      result
    end
    
    def parseWSImpl(die)
      while @idx!=@str.size
        case @str[@idx]
        when ?\n
          @line+=1
        when ?\s, ?\r, ?\t
          # do nothing
        when ?#
          loop {
            if @idx==@str.size
              @idx-=1 # make sure we don't fall off the end
              break
            end
            if @str[@idx]==?\n
              break
            end
            @idx+=1
          }
        else
          if die
            parseError
          else
            return
          end
        end
        @idx+=1
      end
    end
    
    def parseWS
      parseWSImpl(true)
    end
    
    def proceedThroughWS
      parseWSImpl(false)
    end
    
    def proceed
      @idx+=1
      assertNotEOF
    end
    
    def assertNotEOF
      if @idx==@str.size
        parseError
      end
    end
    
    def parseError
      raise "Badly formed input at line #{@line} (text offset #{@idx}): #{@str}"
    end
  end
  
  def self.parse(str)
    Parser.new(str,1).parse
  end
  
  def self.dump(conf)
    Util::dumpShort(conf)
  end
  
  def self.dumpPrettyWithMsg(conf,msg)
    "# "+msg+$/+Util::dumpPretty(conf,0,4)
  end
  
  def self.dumpPretty(conf)
    FijiConfig::dumpPrettyWithMsg(conf,"generated with fijiconfig.rb by #{$0}")
  end
  
  def self.toBoolean(str)
    case str
    when 'true', 'True', 'TRUE', 'yes', 'Yes', 'YES', '1'
      true
    else
      false
    end
  end
  
end


