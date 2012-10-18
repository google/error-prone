package com.google.errorprone.bugpatterns;
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

/**
 * Positive cases for {@link UnneededConditionalOperator}.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class UnneededConditionalOperatorPositiveCases {

  public static void positiveCaseTrueFalseSimple() {
    //BUG: Suggestion includes "isFoo()"
    boolean t = isFoo() ? true : false;
  }

  public static void positiveCaseTrueFalseBinary() {
    //BUG: Suggestion includes "(4 > 5)"
    boolean t = (4 > 5) ? true : false;
  }

  public static void positiveCaseFalseTrueSimple() {
    //BUG: Suggestion includes "!isFoo()"
    boolean t = isFoo() ? false : true;
  }

  public static void positiveCaseFalseTrueUnary() {
    //BUG: Suggestion includes "isFoo()"
    boolean t = !isFoo() ? false : true;
  }

  public static void positiveCaseFalseTrueBinary() {
    //BUG: Suggestion includes "(4 <= 5)"
    boolean t = (4 > 5) ? false : true;
  }

  public static void positiveCaseFalseTrueBinary2() {
    //BUG: Suggestion includes "(4 != 5)"
    boolean t = (4 == 5) ? false : true;
  }

  public static void positiveCaseFalseTrueBinaryUnparenthesised() {
    //BUG: Suggestion includes "4 > 5"
    boolean t = 4 <= 5 ? false : true;
  }

  public static void positiveCaseFalseTrueBinaryUnparenthesised2() {
    //BUG: Suggestion includes "4 == 5"
    boolean t = 4 != 5 ? false : true;
  }

  public static void positiveCaseFalseTrueDeMorgan() {
    //BUG: Suggestion includes "(4 == 5 && 2 != 3)"
    boolean t = (4 != 5 || 2 == 3) ? false : true;
  }

  public static void positiveCaseTrueTrue() {
    //BUG: Suggestion includes "true"
    boolean t = isFoo() ? true : true;
  }

  public static void positiveCaseFalseFalse() {
    //BUG: Suggestion includes "false"
    boolean t = isFoo() ? false : false;
  }

  /** Helper method */
  private static boolean isFoo() {
    return true;
  }
}
