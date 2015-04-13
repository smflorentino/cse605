#!/usr/bin/env ruby

txt=File.read("notices/FPL_notice.txt")

File.open("notices/FPL_notice.java.in","w") {
  | outp |
  outp.puts "/*"
  txt.each_line {
    | line |
    outp.puts " * #{line.chomp}"
  }
  outp.puts " */"
  outp.puts
}


File.open("notices/FPL_notice.rb.in","w") {
  | outp |
  outp.puts "#"
  txt.each_line {
    | line |
    outp.puts "# #{line.chomp}"
  }
  outp.puts "#"
  outp.puts
}

