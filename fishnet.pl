#!/usr/local/bin/perl

# Simple script to start Fishnet

main();

sub main {
    
    $classpath = "lib/:proj/";
    
    $fishnetArgs = join " ", @ARGV;

    exec("java -cp $classpath Fishnet $fishnetArgs");
    # exec("nice java -cp $classpath Fishnet $fishnetArgs");
}

