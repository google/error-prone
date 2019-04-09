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

/** Positive test cases for {@link LiteEnumValueOf} check. */
public class LiteEnumValueOfPositiveCases {

  @SuppressWarnings("StaticQualifiedUsingExpression")
  public static void checkCallOnValueOf() {
    // BUG: Diagnostic contains: LiteEnumValueOf
    ProtoLiteEnum.valueOf("FOO");

    // BUG: Diagnostic contains: LiteEnumValueOf
    ProtoLiteEnum.FOO.valueOf("FOO");
  }

  enum ProtoLiteEnum implements com.google.protobuf.Internal.EnumLite {
    FOO(1),
    BAR(2);
    private final int number;

    private ProtoLiteEnum(int number) {
      this.number = number;
    }

    @Override
    public int getNumber() {
      return number;
    }
  }
}
