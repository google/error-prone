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

import static org.junit.Assert.assertNotNull;

import com.sun.tools.javac.tree.JCTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UTemplater}'s support for basic syntactic constructs.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UTemplaterTest extends AbstractUTreeTest {
  /**
   * Parses a Java expression, templates it using a {@link UTemplater}, and tests that the template
   * matches the original parsed expression.
   */
  private void testTemplateWithoutTypes(String expression) {
    JCTree ast = parseExpression(expression);
    UTree<?> template = (UTree<?>) new UTemplater(context).template(ast);
    assertNotNull(template.unify(ast, unifier));
  }

  @Test
  public void literal() {
    testTemplateWithoutTypes("5L");
  }

  @Test
  public void parens() {
    testTemplateWithoutTypes("(5L)");
  }

  @Test
  public void binary() {
    testTemplateWithoutTypes("\"count:\" + 5");
  }

  @Test
  public void conditional() {
    testTemplateWithoutTypes("true ? 3.14 : -5");
  }

  @Test
  public void unary() {
    testTemplateWithoutTypes("~7");
  }
}
