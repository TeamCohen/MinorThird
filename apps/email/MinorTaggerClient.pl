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
    $fh = new FileHandle "$file";
} else {
    $fh = *STDIN;
}

if ($opt_f ||$opt_F) {
    @all = <$fh>;
    $fileText = join(/\n/, @all);
    $exit = "\$\$\$ \n\n";
    $all = $fileText.$exit;
    $trial = "Hello my name is Cammie Williams. \$\$\$ \n\n";
    sendMessage($socket, $all);
    readMessage($socket);
}

if ((!$file) || $opt_v) {
    print "$remote_host:$remote_port> ";
}

sub readMessage {
    my ($socket) = @_;

    #Opens out file if option o is defined
    if ($opt_o){
      open(OUT, ">./$opt_o");
    } else {
      $isOut = 1;
    }

    $socket->recv($text,1);
    $recievedText = $text;
    while($text ne '')
      {
	$socket->recv($text,16);
	$recievedText = $recievedText.$text;
      }

    #prints to console or to OUT file
    if($isOut == 1) {
      print "$recievedText";
    }
    else {
      print OUT "$recievedText";
    }
    close(OUT);
}

sub sendMessage {
    my ($socket, $message) = @_;
    chomp $message;
    my $len = length $message;
    print $socket "$len $message";
}

