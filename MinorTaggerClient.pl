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

    die "Usage: [-f file] [-F file] [-o output file] [-q] host port\n-q single quotes the input\n-f file takes input line-by-line from file\n-F file takes the whole file as input\n";

}

$| = 1;

$remote_host = $ARGV[0];

if ($remote_host eq "applepie") {
    $remote_host = "128.2.87.186";
} elsif ($remote_host eq "la") {
    $remote_host = "128.2.205.83";
} elsif ($remote_host eq "iim") {
    $remote_host = "128.2.178.246";
} elsif ($remote_host eq "raff") {
    $remote_host = "128.2.205.132";
} elsif ($remote_host eq "cammie"){
    $remote_host = "128.2.191.175";
}

$remote_port = $ARGV[1];

$socket = new IO::Socket::INET (PeerAddr => $remote_host,
                                PeerPort => $remote_port,
                                Proto => "tcp",
                                Type => SOCK_STREAM)
    or die "Can't connect to $remote_host:$remote_port : $!\n";

#$socket = IO::Socket::INET->new("$remote_host:$remote_port") 
#    or die "Can't connect to $remote_host:$remote_port : $!\n";

*STDIN->autoflush(1);

if ($opt_f || $opt_F) {
    $fh = new FileHandle "$file";
} else {
    $fh = *STDIN;
}

if ($opt_F) {
    @all = <$fh>;
    $all = join(/\n/, @all);
    sendMessage($socket, $all);
    print readMessage($socket);
}

if ((!$file) || $opt_v) {
    print "$remote_host:$remote_port> ";
}

while ($line = <$fh>) {
    if ($line =~ /^\s*exit\s*$/) {
        last;
    }
    if ($line =~ /^\s*quit\s*$/) {
	last;
    }
    if ($opt_q) {
        chomp $line;
        $line = "'$line'\n";
    }
    if ($opt_v) {
        print $line;
    }

    $line =~ s:\\n:\n:g;

    print $line;
    sendMessage($socket, $line);
    $line = readMessage($socket);
    print $line, "\n";
    if (!$file) {
        print "$remote_host:$remote_port> ";
    }

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

