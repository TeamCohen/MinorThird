#!/usr/bin/perl
### LBJ2/frontend/getTerminals.pl.  Generated from getTerminals.pl.in by configure.
### --------------------------------------------------------------------------
### Author: Nick Rizzolo
### Usage:
###   getTerminals.pl <JLex file> [<max line length>]
### Description:
###   This script is used to create a list of terminal declarations in CUP's
###   syntax given a JLex file that presupposes CUP's creation of a "sym"
###   class from the terminal declarations in a CUP specification.
### --------------------------------------------------------------------------


die "usage: getTerminals.pl <JLex file> [<max line length = 78>]\n"
  unless @ARGV == 1 || @ARGV == 2;
$maxLength = (@ARGV == 1) ? 78 : pop;

while (<>)
{
  if (($terminal) = (/sym\.([^\s,]+)/))
  {
    $terminals{$terminal} = 1 if ($terminal ne "error" && $terminal ne "EOF");
  }
}

print "terminal TokenValue\n";
$line = " ";
foreach (sort keys %terminals)
{
  if (length($line) + length($_) + 2 <= $maxLength) { $line .= " $_," }
  else
  {
    print "$line\n";
    $line = "  $_,";
  }
}

$line =~ s/,$/;/;
print "$line\n\n";

