// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Refactor {

  /**
   * Entry point for compiling Java code with error-prone enabled.
   * All default checkers are run, and the compile fails if they find a bug.
   * @param args the same args which could be passed to javac on the command line
   */
  public static void main(String[] args) {
    System.exit(new ErrorProneCompiler.Builder().build().compile(args));
  }
}
