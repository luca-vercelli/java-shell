# java-shell
Java library for unix commands, pipelines, shell extension of parameters.

With this library, you can launch commands as

cat("*.txt").pipe(grep("somthg")).pipe(grep_v("somemore")).redirect("output.txt").start()

This emulates UNIX/Linux command line features:
(*) parameters expansion
(*) pipelines of processes
(*) && and || operators among processes
