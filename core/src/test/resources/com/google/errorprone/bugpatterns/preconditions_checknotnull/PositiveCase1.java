package com.google.errorprone.bugpatterns.preconditions_checknotnull;

import com.google.common.base.Preconditions;

public class PositiveCase1 {
  public void error() {
    Preconditions.checkNotNull("string literal");   //BUG
    String thing = null;
    Preconditions.checkNotNull("thing is null", thing);     //BUG
    Preconditions.checkNotNull("a string literal " + "that's got two parts", thing);    //BUG
  }
}