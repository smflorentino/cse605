#!/usr/bin/env ruby

require 'pathname'

def mywrite(flnm,txt)
  $stderr.puts "modifying #{flnm}"
  File.open(flnm,'w') {
    | outp |
    outp.puts(txt)
  }
end

ARGV.each {
  | flnm |
  if File.read(flnm)!~/FIJI PUBLIC LICENSE/
    if flnm=~/\.java$/ or flnm=~/\.c$/ or flnm=~/\.h$/
      mywrite(flnm,File.read("notices/FPL_notice.java.in").gsub(/@NAME_OF_FILE@/,Pathname.new(flnm).basename.to_s)+File.read(flnm))
    elsif flnm=~/\.S$/ or flnm=~/\.rb$/ or flnm=~/rc$/ or File.read(flnm)=~/\/usr\/bin\/env ruby/
      mywrite(flnm,File.read("notices/FPL_notice.rb.in").gsub(/@NAME_OF_FILE@/,Pathname.new(flnm).basename.to_s)+File.read(flnm))
    else
      $stderr.puts "skipping file #{flnm}: unknown extension"
    end
  else
    $stderr.puts "skipping file #{flnm}: already has the notice"
  end
}

