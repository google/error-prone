/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UEnhancedForLoop}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UEnhancedForLoopTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UEnhancedForLoop.create(
                UVariableDecl.create("c", UPrimitiveTypeTree.CHAR, null),
                UMethodInvocation.create(
                    UMemberSelect.create(
                        ULiteral.stringLit("foo"),
                        "toCharArray",
                        UMethodType.create(UArrayType.create(UPrimitiveType.CHAR)))),
                USkip.INSTANCE))
        .addEqualityGroup(
            UEnhancedForLoop.create(
                UVariableDecl.create("c", UClassIdent.create("java.lang.Character"), null),
                UMethodInvocation.create(
                    UMemberSelect.create(
                        ULiteral.stringLit("foo"),
                        "toCharArray",
                        UMethodType.create(UArrayType.create(UPrimitiveType.CHAR)))),
                USkip.INSTANCE))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UEnhancedForLoop.create(
            UVariableDecl.create("c", UPrimitiveTypeTree.CHAR, null),
            UMethodInvocation.create(
                UMemberSelect.create(
                    ULiteral.stringLit("foo"),
                    "toCharArray",
                    UMethodType.create(UArrayType.create(UPrimitiveType.CHAR)))),
            USkip.INSTANCE));
  }
}
