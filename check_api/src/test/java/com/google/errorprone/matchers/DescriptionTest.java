/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class DescriptionTest {

  private static class MockTree implements Tree, DiagnosticPosition {
    @Override
    public <R, D> R accept(TreeVisitor<R, D> arg0, D arg1) {
      return null;
    }

    @Override
    public Kind getKind() {
      return null;
    }

    @Override
    public JCTree getTree() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getStartPosition() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getPreferredPosition() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getEndPosition(EndPosTable endPosTable) {
      throw new UnsupportedOperationException();
    }
  }

  @BugPattern(
      name = "DeadException",
      summary = "Exception created but not thrown",
      explanation = "",
      category = JDK,
      severity = ERROR)
  public static class MyChecker extends BugChecker {
    Description getDescription() {
      return describeMatch(new MockTree());
    }
  }

  private static final String URL = "  (see https://errorprone.info/bugpattern/DeadException)";

  @Test
  public void testDescriptionFromBugPattern() {
    Description description = new MyChecker().getDescription();
    assertEquals("DeadException", description.checkName);
    assertEquals(
        "Exception created but not thrown\n" + URL, description.getMessageWithoutCheckName());
    assertEquals(
        "[DeadException] Exception created but not thrown\n" + URL, description.getMessage());
  }

  @Test
  public void testCustomDescription() {
    Description description =
        BugChecker.buildDescriptionFromChecker((DiagnosticPosition) new MockTree(), new MyChecker())
            .setMessage("custom message")
            .build();
    assertEquals("DeadException", description.checkName);
    assertEquals("custom message\n" + URL, description.getMessageWithoutCheckName());
    assertEquals("[DeadException] custom message\n" + URL, description.getMessage());
  }

  @BugPattern(
      name = "CustomLinkChecker",
      summary = "Exception created but not thrown",
      explanation = "",
      category = JDK,
      severity = ERROR,
      linkType = CUSTOM,
      link = "https://www.google.com/")
  public static class CustomLinkChecker extends BugChecker {
    Description getDescription() {
      return describeMatch(new MockTree());
    }
  }

  @Test
  public void testCustomLink() {
    Description description =
        BugChecker.buildDescriptionFromChecker(
                (DiagnosticPosition) new MockTree(), new CustomLinkChecker())
            .setMessage("custom message")
            .build();
    assertEquals(
        "[CustomLinkChecker] custom message\n  (see https://www.google.com/)",
        description.getMessage());
  }
}
