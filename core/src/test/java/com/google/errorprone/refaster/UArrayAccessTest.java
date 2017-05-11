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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UArrayAccess}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UArrayAccessTest extends AbstractUTreeTest {
  @Test
  public void unify() {
    UExpression arrayIdent = mock(UExpression.class);
    when(arrayIdent.unify(ident("array"), isA(Unifier.class))).thenReturn(Choice.of(unifier));
    assertUnifies("array[5]", UArrayAccess.create(arrayIdent, ULiteral.intLit(5)));
  }

  @Test
  public void inline() throws CouldNotResolveImportException {
    UExpression arrayIdent = mock(UExpression.class);
    when(arrayIdent.inline(isA(Inliner.class)))
        .thenReturn(inliner.maker().Ident(inliner.asName("array")));
    assertInlines("array[5]", UArrayAccess.create(arrayIdent, ULiteral.intLit(5)));
  }
}
