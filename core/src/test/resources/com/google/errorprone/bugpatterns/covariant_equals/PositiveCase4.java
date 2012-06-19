// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.bugpatterns.covariant_equals;

import java.lang.String;

/**
 * Defining an equals method on an enum. Maybe this should be a separate kind of error?
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public enum PositiveCase4 {
  MERCURY,
  VENUS,
  EARTH,
  MARS,
  JUPITER,
  SATURN,
  URANUS,
  NEPTUNE,
  PLUTO;   // I don't care what they say, Pluto *is* a planet.
  
  public boolean equals(PositiveCase4 other) {
    return this == other;
  }
}