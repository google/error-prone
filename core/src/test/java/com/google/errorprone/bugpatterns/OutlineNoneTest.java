/*
 * Copyright 2018 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link OutlineNone} */
@RunWith(JUnit4.class)
public final class OutlineNoneTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(OutlineNone.class, getClass());

  @Test
  public void template() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.gwt.safehtml.shared.SafeHtml;",
            "import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;",
            "interface Test {",
            "  // BUG: Diagnostic contains: OutlineNone",
            "  @Template(\".body {color: red;outline: 0px;}"
                + "<a href=http://outlinenone.com style=\\\"outline:none\\\">\")",
            "  SafeHtml myElement();",
            "  // BUG: Diagnostic contains: OutlineNone",
            "  @Template(\".invisible {outline: none}\")",
            "  SafeHtml invisible();",
            "}")
        .doTest();
  }

  @Test
  public void templateMutliline() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.gwt.safehtml.shared.SafeHtml;",
            "import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;",
            "interface Test {",
            "  // BUG: Diagnostic contains: OutlineNone",
            "  @Template(\".body {color: red;}\\n\"",
            "      + \"<a href=http://outlinenone.com style=\\\"outline:none\\\">\")",
            "  SafeHtml myElement();",
            "}")
        .doTest();
  }

  @Test
  public void gwtSetProperty() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.gwt.dom.client.Style;",
            "class Test {",
            "  private static final String OUTLINE = \"outline\";",
            "  private static final double d = 0.0;",
            "  void test(Style s) {",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, \"none\");",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setPropertyPx(\"outline\", 0);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, d, Style.Unit.PX);",
            "    s.setPropertyPx(OUTLINE, 1);", // No bug
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void gwtSetProperty_numberTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.gwt.dom.client.Style.Unit.PX;",
            "import com.google.gwt.dom.client.Style;",
            "class Test {",
            "  private static final String OUTLINE = \"outline\";",
            "  private static final int ZEROI = 0;",
            "  private static final long ZEROL = 0L;",
            "  private static final short ZEROS = 0;",
            "  private static final double ZEROD = 0.0d;",
            "  private static final float ZEROF = 0.0f;",
            "  private static final double NEG_ZERO = -0.0;",
            "  void test(Style s) {",
            // setProperty(String, double, Unit)
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, ZEROI, PX);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, ZEROL, PX);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, ZEROS, PX);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, ZEROD, PX);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, ZEROF, PX);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setProperty(OUTLINE, NEG_ZERO, PX);",
            // setProperty(String, int)
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setPropertyPx(OUTLINE, ZEROI);",
            "    // BUG: Diagnostic contains: OutlineNone",
            "    s.setPropertyPx(OUTLINE, ZEROS);",
            // non-zero, no bugs
            "    s.setProperty(OUTLINE, 0.00001d, PX);",
            "    s.setProperty(OUTLINE, 0.00001f, PX);",
            "  }",
            "}")
        .doTest();
  }
}
