#!/usr/bin/env ruby

size=ARGV.shift
threads=ARGV.shift
duration=ARGV.shift
runs=ARGV.shift

vmcmds=ARGV

thrs=[]

vmcmds.each {
  | vmcmd |
  thr=nil
  cmd="#{vmcmd} -cp lib/fivmtest.jar:lib/fivmcommon.jar com.fiji.fivm.test.BiasedLockExplosion #{size} #{threads} #{duration} #{runs} #{runs}"
  $stderr.puts cmd
  IO.popen(cmd) {
    | inp |
    inp.each_line {
      | line |
      $stderr.puts line
      if line=~/RESULT/
        if line=~/Avg:([Ee.0-9]+)/
          thr=$1.to_f
        end
      end
    }
  }
  unless thr and thr!=0
    raise "That failed"
  end
  thrs << thr
  $stderr.puts(thrs.join(' '))
}

puts(thrs.join(' '))
