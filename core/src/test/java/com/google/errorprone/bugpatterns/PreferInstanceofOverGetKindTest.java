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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class PreferInstanceofOverGetKindTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(PreferInstanceofOverGetKind.class, getClass());

  @Test
  public void positive() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree.getKind() == Tree.Kind.MEMBER_SELECT;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.sun.source.tree.MemberSelectTree;
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree instanceof MemberSelectTree;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void instanceEquals() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree.getKind().equals(Tree.Kind.MEMBER_SELECT);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.sun.source.tree.MemberSelectTree;
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree instanceof MemberSelectTree;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void instanceEquals_negated() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return !tree.getKind().equals(Tree.Kind.MEMBER_SELECT);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.sun.source.tree.MemberSelectTree;
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return !(tree instanceof MemberSelectTree);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negated() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree.getKind() != Tree.Kind.MEMBER_SELECT;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.sun.source.tree.MemberSelectTree;
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return !(tree instanceof MemberSelectTree);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleResolveToBinaryTree_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isMemberSelect(Tree tree) {
                return tree.getKind() == Tree.Kind.EQUAL_TO;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void comparedToWrongType_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.sun.source.tree.Tree;

            class Test {
              boolean isWrongType(Tree tree) {
                return tree.getKind().equals(1);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
