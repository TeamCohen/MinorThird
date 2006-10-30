Data used to test the perl scripts 

 *.txt in the directory - various input/output files for CASS
 textbase

 textbase/cass{1,2}-in, textbase/cass{1,2,2a}-out - contains sample
 input to and output from cass, as a minorthird labels file

 textbase/test-plain, textbase/test-xml - really tiny test directories

 textbase/wc-* - minorthird labels files containing a single test file
 in various forms:

  wc-plain = plain text
  wc-tagged = output of SNOW POS tagged
  wc-chunked = output of SNOW Shallow parser
  wc-cass = SNOW-tagged version of test file, adapted and then run through CASS
  wc-xml = wc-chunked, converted to xml with perl/snowchunk2xml.pl
  wc-cassxml = wc-cass,  converted to xml with perl/cass2xml.pl




