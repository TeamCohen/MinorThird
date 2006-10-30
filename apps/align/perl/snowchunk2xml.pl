#translate the output of the snow-based shallow parser to a
#minorthird-compatible XML format

while ($line = <>) {
    $line =~ s/\[(\S+) ([^\]]+) \]/<$1>$2<\/$1>/g;
    $line =~ s/\((\S+) (\S+)\)/<$1>$2<\/$1>/g;
    $line =~ s/PP\$/PPD/g;
    $line =~ s/\s+/ /g;
    print "<sentence>$line</sentence>\n";
}

