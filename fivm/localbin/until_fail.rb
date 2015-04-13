#!/usr/bin/env ruby

$stderr.puts "Executing command: #{ARGV.inspect}"

before=Time.now

cnt=1

loop {
  unless system(*ARGV)
    after=Time.now
    result=$?
    $stderr.puts "FAILED on run \##{cnt}."
    $stderr.puts "Result: #{result.inspect}"
    $stderr.puts "Command: #{ARGV.inspect}"
    $stderr.puts "Ran #{cnt} executions for #{after-before} seconds."
    exit 1
  end
  cnt+=1
}


