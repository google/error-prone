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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class StringLiteralTest {
  @Test public void matches() {
    LiteralTree tree = mock(LiteralTree.class);
    when(tree.getValue()).thenReturn("a string literal");
    assertTrue(new StringLiteral("a string literal").matches(tree, null));
  }
  
  @Test public void notMatches() {
    LiteralTree tree = mock(LiteralTree.class);
    when(tree.getValue()).thenReturn("a string literal");
    assertFalse(new StringLiteral("different string").matches(tree, null));
    
    IdentifierTree idTree = mock(IdentifierTree.class);
    assertFalse(new StringLiteral("test").matches(idTree, null));
    
    LiteralTree intTree = mock(LiteralTree.class);
    when(intTree.getValue()).thenReturn(5);
    assertFalse(new StringLiteral("test").matches(intTree, null));
  }
}
