// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

/**
 * An annotation intended for implementations of Matcher which is picked up by our 
 * documentation processor.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public @interface BugPattern {

  /**
   * Name should be unique
   */
  String name();
  
  Category category();
  
  public enum Category {
    JDK, GUAVA, APPLICATION_SPECIFIC
  }

  /**
   * Wiki syntax not allowed
   */
  String summary();
  
  /**
   * Wiki syntax allowed
   */
  String explanation();

  SeverityLevel severity();

  public enum SeverityLevel {
    WARNING, ERROR
  }

  MaturityLevel maturity();
  
  public enum MaturityLevel {
    ON_BY_DEFAULT,
    EXPERIMENTAL,
    PROPOSED,
  }
}
