#!/usr/bin/ruby

from=ARGV.shift
to=ARGV.shift

ARGV.each {
	| filename |
	text=nil
	File.open(filename,'r') {
		| io |
		text=io.read
	}
	if text.gsub!(from,to)
		puts "Saving #{filename}..."
		File.open(filename,'w') {
			| io |
			io.write(text)
		}
	end
}

