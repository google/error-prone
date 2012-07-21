// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.bugpatterns.covariant_equals;

import java.lang.String;

/**
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCase3 {

  boolean isInVersion;
  String whitelist;

  public boolean equals(PositiveCase3 that) {   //BUG
    return ((this.isInVersion == that.isInVersion) &&
            this.whitelist.equals(that.whitelist));
  }

}
