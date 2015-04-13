require 'pathname'
require 'getoptlong'

require 'config'
require 'fijiconfig'
require 'testlib'

opts=GetoptLong.new([ '--output', GetoptLong::REQUIRED_ARGUMENT ],
                    [ '--only', GetoptLong::REQUIRED_ARGUMENT ],
                    [ '--quiet', GetoptLong::NO_ARGUMENT ],
                    [ '--verbose', GetoptLong::NO_ARGUMENT ])

$output='tester-output.conf'
$dotsOnly=false
$verbose=false
$runPattern=//

opts.each {
  | opt, arg |
  case opt
  when '--only'
    $runPattern=Regexp.compile(arg)
  when '--verbose'
    $verbose=true
  when '--quiet'
    $dotsOnly=true
  when '--output'
    $output=arg
  end
}


