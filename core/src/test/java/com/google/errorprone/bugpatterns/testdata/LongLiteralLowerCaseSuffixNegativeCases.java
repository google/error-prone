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

/**
 * Negative cases for {@link LongLiteralLowerCaseSuffix}
 * 
 * @author Simon Nickerson (sjnickerson@google.com)
 */
public class LongLiteralLowerCaseSuffixNegativeCases {
  public void positiveUpperCase() {
    long value = 123432L;
  }
  
  public void zeroUpperCase() {
    long value = 0L;
  }

  public void negativeUpperCase() {
    long value = -3L;
  }
  
  public void notLong() {
    String value = "0l";
  }
  
  public void variableEndingInEllIsNotALongLiteral() {
    long ell = 0L; 
    long value = ell;
  }
  
  public void positiveNoSuffix() {
    long value = 3; 
  }
  
  public void negativeNoSuffix() {
    long value = -3;
  }
  
  public void positiveHexUpperCase() {
    long value = 0x80L;
  }
  
  public void zeroHexUpperCase() {
    long value = 0x0L;
  }
  
  public void negativeHexUpperCase() {
    long value = -0x80L;
  }

}
