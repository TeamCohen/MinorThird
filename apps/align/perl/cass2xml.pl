#translate the output of the CASS partial parser to a
#minorthird-compatible XML format

@tags = ();
$tag = 'ERROR';
$buf = '';
while ($line = <>) {
    chop;
    $found = 1;
    while ($found) {
	$found = 0;
	if ($line =~ s/\[(\S+)/<$1>/)  {
	    $tag = $1;
	    push(@tags,$tag);
	    $found = 1;
	}
	if ($line =~ m/\]/) {
	    $tag = pop(@tags);	    
	    $line =~ s/\]/<\/$tag>/;
	    $found = 1;
	}
    }
    #remove extra spaces, and replace the end-sentence tag with a newline
    $line =~ s/^\s*\.\s+(\W)\S*$/$1/;
    $buf .= $line;
    if ($line=~m[</s>]) {
	$buf =~ s/\>\s+\</></g;
	$buf =~ s/^\s+//g;
	$buf =~ s/\s+/ /g;
	print $buf,"\n";
	$buf = '';
    }
}
