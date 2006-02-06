while (<>) {
  ($id,$rest) = $_ =~ /(\S+) (.*)/;
  open(F,">caption-lines/$id") || die;
  print F $rest;
  close(F);
}
