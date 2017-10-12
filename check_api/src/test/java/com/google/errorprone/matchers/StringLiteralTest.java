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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;
import javax.lang.model.element.Name;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class StringLiteralTest {
  @Test
  public void matches() {
    // TODO(b/67738557): consolidate helpers for creating fake trees
    LiteralTree tree =
        new LiteralTree() {

          @Override
          public Kind getKind() {
            throw new UnsupportedOperationException();
          }

          @Override
          public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
            throw new UnsupportedOperationException();
          }

          @Override
          public Object getValue() {
            return "a string literal";
          }
        };
    assertTrue(new StringLiteral("a string literal").matches(tree, null));
  }

  @Test
  public void notMatches() {
    // TODO(b/67738557): consolidate helpers for creating fake trees
    LiteralTree tree =
        new LiteralTree() {

          @Override
          public Kind getKind() {
            throw new UnsupportedOperationException();
          }

          @Override
          public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
            throw new UnsupportedOperationException();
          }

          @Override
          public Object getValue() {
            return "a string literal";
          }
        };
    assertFalse(new StringLiteral("different string").matches(tree, null));

    IdentifierTree idTree =
        new IdentifierTree() {
          @Override
          public Name getName() {
            return null;
          }

          @Override
          public Kind getKind() {
            throw new UnsupportedOperationException();
          }

          @Override
          public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
            throw new UnsupportedOperationException();
          }
        };
    assertFalse(new StringLiteral("test").matches(idTree, null));

    // TODO(b/67738557): consolidate helpers for creating fake trees
    LiteralTree intTree =
        new LiteralTree() {
          @Override
          public Object getValue() {
            return 5;
          }

          @Override
          public Kind getKind() {
            throw new UnsupportedOperationException();
          }

          @Override
          public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
            throw new UnsupportedOperationException();
          }
        };
    assertFalse(new StringLiteral("test").matches(intTree, null));
  }
}
