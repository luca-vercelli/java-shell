# java-shell
Java library for scripting in Java.
Give support for unix commands, pipelines, shell extension of parameters.

With this library, you can launch commands like

    cat("*.txt").pipe(grep("somthg")).pipe(grep_v("somemore")).redirect("output.txt").sh()

This emulates UNIX/Linux command line features:

  * parameters expansion
  * pipelines of processes
  * && and || operators among processes
