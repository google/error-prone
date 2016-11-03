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

package com.google.errorprone.bugpatterns.testdata;

/**
 * Positive cases for {@link LongLiteralLowerCaseSuffix}.
 */
public class LongLiteralLowerCaseSuffixPositiveCase1 {
  
  // This constant string includes non-ASCII characters to make sure that we're not confusing
  // bytes and chars:
  @SuppressWarnings("unused")
  private static final String TEST_STRING = "Îñţérñåţîöñåļîžåţîờñ";
  
  public void positiveLowerCase() {
    // BUG: Diagnostic contains: value = 123432L
    long value = 123432l;
  }
  
  public void zeroLowerCase() {
    // BUG: Diagnostic contains: value = 0L
    long value = 0l;
  }
  
  public void negativeLowerCase() {
    // BUG: Diagnostic contains: value = -123432L
    long value = -123432l;
  }
  
  public void negativeExtraSpacesLowerCase() {
    // BUG: Diagnostic contains: value = -  123432L
    long value = -  123432l;
  }
  
  public void positiveHexLowerCase() {
    // BUG: Diagnostic contains: value = 0x8abcDEF0L
    long value = 0x8abcDEF0l;
    // BUG: Diagnostic contains: value = 0X80L
    value = 0X80l;
  }
  
  public void zeroHexLowerCase() {
    // BUG: Diagnostic contains: value = 0x0L
    long value = 0x0l;
    // BUG: Diagnostic contains: value = 0X0L
    value = 0X0l;
  }
  
  public void negativeHexLowerCase() {
    // BUG: Diagnostic contains: value = -0x8abcDEF0L
    long value = -0x8abcDEF0l;
    // BUG: Diagnostic contains: value = -0X80L
    value = -0X80l;
  }
  
  public void negativeHexExtraSpacesLowerCase() {
    // BUG: Diagnostic contains: value = -  0x8abcDEF0L
    long value = -  0x8abcDEF0l;
  }
  
  public void positiveOctalLowerCase() {
    // BUG: Diagnostic contains: value = 06543L
    long value = 06543l;
  }
  
  public void zeroOctalLowerCase() {
    // BUG: Diagnostic contains: value = 00L
    long value = 00l;
  }
  
  public void negativeOctalLowerCase() {
    // BUG: Diagnostic contains: value = -06543L
    long value = -06543l;
  }
  
  public void negativeOctalExtraSpacesLowerCase() {
    // BUG: Diagnostic contains: value = -  06543L
    long value = -  06543l;
  }

}
