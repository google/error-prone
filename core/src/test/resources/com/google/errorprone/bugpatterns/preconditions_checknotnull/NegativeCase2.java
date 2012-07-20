package com.google.errorprone.bugpatterns.preconditions_checknotnull;

import com.google.common.base.Preconditions;

public class NegativeCase2 {
  public void go() {
    Object testObj = null;
    Preconditions.checkNotNull(testObj, "this is ok");
  }  
}