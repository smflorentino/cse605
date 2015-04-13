raise unless system("java -Xmx768M -classpath ecj\\ecj-3.5.1.jar org.eclipse.jdt.internal.compiler.batch.Main -warn:-deadCode "+ARGV.collect{|x| x.inspect}.join(' '))

