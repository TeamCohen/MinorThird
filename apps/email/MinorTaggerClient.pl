#!/usr/local/bin/perl

use IO::Socket;
use FileHandle;
use Getopt::Std;

getopts('vf:qF:o:');

if ($opt_f) {
    $file = $opt_f;
} 

if ($opt_F) {
    $file = $opt_F;
}
if ($opt_o) {
  $out = $opt_o;
}

$quote = $opt_q;

if ($#ARGV != 1) {

    die "Usage: [-F file] [-o output file]  host port 
\nREMINDER: All input file must end with: \$\$\$ \nHost Names: roxie and velmak \nBoth hosts are running on port 9998 \n";

}

$| = 1;

$remote_host = $ARGV[0];

if ($remote_host eq "raff") {
    $remote_host = "128.2.205.132";
} elsif ($remote_host eq "cammie"){
    $remote_host = "128.2.191.175";
  } elsif ($remote_host eq "roxie") {
    $remote_host = "128.2.209.54";
  } elsif ($remote_host eq "velmak") {
    $remote_host = "128.2.209.57";
  }

$remote_port = $ARGV[1];

$socket = new IO::Socket::INET (PeerAddr => $remote_host,
                                PeerPort => $remote_port,
                                Proto => "tcp",
                                Type => SOCK_STREAM)
    or die "Can't connect to $remote_host:$remote_port : $!\nREMINDER: Both roxie and velmak are running on port 9998 \n";

#$socket = IO::Socket::INET->new("$remote_host:$remote_port") 
#    or die "Can't connect to $remote_host:$remote_port : $!\n";

*STDIN->autoflush(1);

print "REMINDER: \nTo Use MinorTaggerClient your file must end with: \$\$\$ \nHost Names: roxie and velmak \nBoth hosts are running on port 9998 \n";

if ($opt_f || $opt_F) {
#    $fh = new FileHandle "$file";
#     @all = <$fh>;
    open(F,"<$file") || die;
    @all = <F>;
    $fileText = join('',@all);
    $exit = "\nexit";
    $all = $fileText.$exit;
    print "sending: '$all' \n";
    sendMessage($socket, $all);
    print "sent message\n";
    readMessage($socket);
    print "exiting\n";
    exit;
} else {
  print "usage: -f file\n";
}

sub readMessage {
    my ($socket) = @_;
    my $in;
    my $len = ""; 
    my $done;
    my $out = "";
    $isOut = 0;
    if ($opt_o){
      open(OUT, ">./$opt_o");
    } else {
      $isOut = 1;
    }

    $done = 0;
  LENGTH:
    while (!$done) {
        if (!defined(recv($socket, $in, 1, 0))) {
            print "Quitting?\n";
            exit(0);
        }
	if("$in" eq "|") {
	  $done = 1;
	} else {
	  if($isOut == 1){
	    print "$in";
	  } else {
	    print OUT "$in";
	  }
	}
	

        #if (length($in) != 1) {
        #    exit(0);
        #}

        #if ($in ne " ") {
        #    $len = $len . $in;
        #} else {
        #    $done = 1;
        #}
      }
    close(OUT);
    if ($len =~ /^\d+$/) {
      #        print STDERR "received number $len\n";
      while ($len > 0) {
	  #	    print STDERR "expecting $len bytes\n";
	  recv($socket, $in, $len, 0);
	    $receivedBytes = length($in);
	    #	    print STDERR "received $receivedBytes\n";
	    $len = $len - $receivedBytes;
	    $out = $out . $in;
	  }
	return $out;
    } else {
        goto LENGTH;
    }
}

sub sendMessage {
    my ($socket, $message) = @_;
    chomp $message;
    my $len = length $message;
#    print "sending $len $message\n";
    print $socket "$len $message";
}

