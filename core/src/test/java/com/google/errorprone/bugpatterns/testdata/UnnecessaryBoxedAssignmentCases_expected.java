/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

/** @author awturner@google.com (Andy Turner) */
class UnnecessaryBoxedAssignmentCases {
  void negative_void() {
    return;
  }

  boolean positive_booleanPrimitive(boolean aBoolean) {
    return aBoolean;
  }

  Boolean positive_booleanWrapped(boolean aBoolean) {
    Boolean b = aBoolean;
    return aBoolean;
  }

  Boolean negative_booleanString(String aString) {
    Boolean b = Boolean.valueOf(aString);
    return Boolean.valueOf(aString);
  }

  byte positive_bytePrimitive(byte aByte) {
    return aByte;
  }

  Byte positive_byteWrapped(byte aByte) {
    Byte b = aByte;
    return aByte;
  }

  Byte negative_byteString(String aString) {
    Byte b = Byte.valueOf(aString);
    return Byte.valueOf(aString);
  }

  int positive_integerPrimitive(int aInteger) {
    return aInteger;
  }

  Integer positive_integerWrapped(int aInteger) {
    Integer i = aInteger;
    return aInteger;
  }

  Integer negative_integerString(String aString) {
    Integer i = Integer.valueOf(aString);
    return Integer.valueOf(aString);
  }

  Long negative_integerWrapped(int aInteger) {
    Long aLong = Long.valueOf(aInteger);
    return Long.valueOf(aInteger);
  }

  Integer positive_wrappedAgain(int aInteger) {
    Integer a = aInteger;
    a = aInteger;
    return a;
  }
}
