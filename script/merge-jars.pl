#!/usr/bin/perl

##############################################################################
#
#create a single jar file, which merges  the contents of lib/*.jar
#
##############################################################################

my $mainJar = 'minorThirdIncludes.jar';

system("mkdir tmp") && die unless (-e "tmp");
system("mkdir tmp/merge") && die unless (-e "tmp/merge");
chdir("lib") || die;
my @allJars = glob("*.jar");
my @jars = ();
foreach my $j (@allJars) {
  next if $j eq $mainJar;
  system("cp $j ../tmp/merge") && die;
  push(@jars,$j);
}
chdir("../tmp/merge") || die;
print "wd is ",`pwd`;
print "jars is ",join(" ",lib),"\n";
foreach my $j (@jars) {
  print "extracting $j into tmp/merge\n";
  system("jar -xf $j") && die;
  system("rm -f $j") && die;
  system("rm -rf META-INF") && die;
}
my $dirs = join(' ',glob("*"));
print "jar command: jar -cvf $mainJar $dirs\n";
system("jar -cf $mainJar $dirs") && die;
system("cp $mainJar ../../lib");
chdir("../..") || die;
system("rm -r tmp/merge") && die;
