#!/usr/bin/env ruby

require 'digest/md5'

ARGV.each {
  | flnm |
  puts "#{Digest::MD5.hexdigest(File.read(flnm))}  #{flnm}"
}

