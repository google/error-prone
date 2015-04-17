/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import java.lang.String;

/**
 * Defining an equals method on an enum. Maybe this should be a separate kind of error?
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public enum CovariantEqualsPositiveCase4 {
  MERCURY,
  VENUS,
  EARTH,
  MARS,
  JUPITER,
  SATURN,
  URANUS,
  NEPTUNE,
  PLUTO;   // I don't care what they say, Pluto *is* a planet.
  
  // BUG: Diagnostic contains: remove this line
  public boolean equals(CovariantEqualsPositiveCase4 other) {
    return this == other;
  }
}