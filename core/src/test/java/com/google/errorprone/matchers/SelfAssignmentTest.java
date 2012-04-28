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

package com.google.errorprone.matchers;

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.AssignmentTree;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SelfAssignmentTest extends CompilerBasedTest {
  @Test public void identifierSelfAssignmentMatches() throws IOException {
    writeFile("A.java",
        "public class A {{",
        "  int a = 0;",
        "  a = a;",
        "}}"
    );
    assertCompiles(assignmentMatches(true, new SelfAssignment()));
  }
  
  @Test public void memberSelectSelfAssignmentMatches() throws IOException {
    writeFile("A.java",
        "public class A {",
        "  public int foo;",
        "}"
    );
    writeFile("B.java",
        "public class B {{",
        "  A a = new A();",
        "  a.foo = a.foo;",
        "}}"
        );
    assertCompiles(assignmentMatches(true, new SelfAssignment()));
  }

  @Test public void matchesFieldSelfAssignment() throws IOException {
    writeFile("A.java",
        "public class A {",
        "  private String foo;",
        "  public A(String foa) {",
        "    this.foo = foo;",
        "  }",
        "}"
    );
    assertCompiles(assignmentMatches(true, new SelfAssignment()));
  }

  @Test public void cannotDetermineWhenIdenticalReferencesAreAssigned() throws IOException {
    writeFile("A.java",
        "public class A {",
        "  public int foo;",
        "}"
    );
    writeFile("B.java",
        "public class B {{",
        "  A a = new A();",
        "  A b = a;",
        // This is self-assignment, but we don't handle this case
        "  a.foo = b.foo;",
        "}}"
    );
    assertCompiles(assignmentMatches(false, new SelfAssignment()));
  }

  private Scanner assignmentMatches(final boolean shouldMatch, final SelfAssignment toMatch) {
    return new Scanner() {
      @Override
      public Void visitAssignment(AssignmentTree node, VisitorState visitorState) {
        assertTrue(node.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        return super.visitAssignment(node, visitorState);
      }
    };
  }
}
