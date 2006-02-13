#!/usr/local/bin/perl

use IO::Socket;
use Getopt::Std;

getopts('o:');

if ($opt_o) {
  $out = $opt_o;
  open(OUT, ">./$opt_o");
}

$remote_host = "128.2.209.57";
$remote_port = "9998";
$file = shift;
$label = shift;

if ($remote_host eq "raff") {
    $remote_host = "128.2.205.132";
  } elsif ($remote_host eq "roxie") {
    $remote_host = "128.2.209.54";
  } elsif ($remote_host eq "velmak") {
    $remote_host = "128.2.209.57";
  }

unless ($file) {
  die "usage: $0 -o output_file input_file output_type[labels/xml]\n";
}

$socket = new 
  IO::Socket::INET (PeerAddr => $remote_host,
		    PeerPort => $remote_port,
		    Proto => 'tcp',
		    Type => SOCK_STREAM)
  or die "Can't connect to $remote_host:$remote_port : $!";

$label .= "\n";
print $socket $label;
$fileName = "***".$file."\n";
print $socket $fileName;
open(F,"<$file") || die "Can't open file $file: $!";
while ($line = <F>) {
  $msg .= $line;
}
$msg =~ s/^\s+//;
$msg =~ s/\s+$//;
#special 'sentinal' line to indicate the end of a message
$msg .= '$$$' . "\n";

#print "message was '",$msg,"'\n";

#send sentinal-terminated message to the server
print $socket $msg;

#get the result back
while ($line = <$socket>) {
  if($opt_o){
    print OUT $line;
  } else {print $line;}
}
