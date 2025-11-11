/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FormatStringShouldUsePlaceholdersTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          FormatStringShouldUsePlaceholders.class, getClass());

  @Test
  public void verifyRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
"""
import com.google.common.base.Verify;

public class Test {
  public void verify(int i, Object o) {
    Verify.verify(false, "Raspberry " + i + " irradiates " + o);
    Verify.verifyNotNull(o, o + " is null");
    Verify.verify(false, "%s" + i);
    Verify.verify(false, "%s%s" + i);
  }
}
""")
        .addOutputLines(
            "Test.java",
"""
import com.google.common.base.Verify;

public class Test {
  public void verify(int i, Object o) {
    Verify.verify(false, "Raspberry %s irradiates %s", i, o);
    Verify.verifyNotNull(o, "%s is null", o);
    Verify.verify(false, "%s", i);
    Verify.verify(false, "%s%s" + i);
  }
}
""")
        .doTest();
  }

  @Test
  public void preconditionsRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
"""
import com.google.common.base.Preconditions;

public class Test {
  private static final String STRING_CONST = "string constant";

  public void preconditions(int i, Object o) {
    Preconditions.checkArgument(false, "Raspberry " + i + " irradiates " + o);
    Preconditions.checkArgument(false, "Concat " + i + " and argument %s", o);
    Preconditions.checkArgument(false, i + " begin and argument %s", o);
    Preconditions.checkArgument(false, "Argument %s and concat " + i, o);
    Preconditions.checkArgument(false, "How about %s the middle " + i + " as well %s?", o, o);
    Preconditions.checkArgument(false, "But string concat %s " + "string is ok", i);
    Preconditions.checkArgument(false, "And string concat " + STRING_CONST + " is ok");
    Preconditions.checkState(false, i + " defenestrates " + o);
    Preconditions.checkNotNull(o);
    Preconditions.checkState(o != null, o);
  }
}
""")
        .addOutputLines(
            "Test.java",
"""
import com.google.common.base.Preconditions;

public class Test {
  private static final String STRING_CONST = "string constant";

  public void preconditions(int i, Object o) {
    Preconditions.checkArgument(false, "Raspberry %s irradiates %s", i, o);
    Preconditions.checkArgument(false, "Concat %s and argument %s", i, o);
    Preconditions.checkArgument(false, "%s begin and argument %s", i, o);
    Preconditions.checkArgument(false, "Argument %s and concat %s", o, i);
    Preconditions.checkArgument(false, "How about %s the middle %s as well %s?", o, i, o);
    Preconditions.checkArgument(false, "But string concat %s " + "string is ok", i);
    Preconditions.checkArgument(false, "And string concat " + STRING_CONST + " is ok");
    Preconditions.checkState(false, "%s defenestrates %s", i, o);
    Preconditions.checkNotNull(o);
    Preconditions.checkState(o != null, o);
  }
}
""")
        .doTest();
  }
}
