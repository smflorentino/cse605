#!/usr/bin/env ruby

duration=ARGV.shift.to_i
limit=ARGV.shift.to_i
vmcmds=ARGV

times=[]

vmcmds.each {
  | vmcmd |
  mytimes=[]
  (0..limit).each {
    | myLimit |
    time=nil
    cmd="#{vmcmd} -cp lib/fivmtest.jar:lib/fivmcommon.jar com.fiji.fivm.test.AllocLock #{duration} #{myLimit}"
    $stderr.puts cmd
    IO.popen(cmd) {
      | inp |
      inp.each_line {
        | line |
        $stderr.puts line
        if line=~/That took ([0-9]+) ms/
          time=$1.to_i
        end
      }
    }
    unless time and time!=0
      raise "That failed"
    end
    mytimes << time
    $stderr.puts(mytimes.join(' '))
  }
  times << mytimes
}

times.each {
  | mytimes |
  puts(mytimes.join(' '))
}
