#!/usr/bin/perl
# LBJ2/frontend/genNameTable.pl.  Generated from genNameTable.pl.in by configure.
# This script generates a Java class "SymbolNames" with a single static member
# array called "nameTable", which maps CUP terminal symbol values to their
# names.

die "usage: genNameTable.pl <CUP generated sym.java file>\n"
  unless @ARGV == 1;

while (<>)
{
  next unless /public static final int/;  # Assume these are the terminals...
  s/;//;
  my ($public,$static,$final,$int,$NAME,$equals,$VALUE) = split;
  $terms[$VALUE] = $NAME;
}

print "package LBJ2.frontend;\n\n"
    . "public class SymbolNames\n"
    . "{\n"
    . "  public static String nameTable[ ] = {\n"
    . "    " . join(", ", map { "\"$_\"" } @terms) . "\n"
    . "  };\n"
    . "}\n";

