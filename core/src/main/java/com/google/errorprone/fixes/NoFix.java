/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.fixes;

import com.google.errorprone.ErrorProneEndPosMap;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * An implementation of {@code Fix} for use as the canonical value when there is no fix.  It
 * is immutable and, when applied, results in no changes to the source code.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class NoFix implements Fix {
  protected NoFix() {}

  @Override
  public String toString(JCCompilationUnit compilationUnit) {
    return "no fix";
  }

  @Override
  public Set<Replacement> getReplacements(ErrorProneEndPosMap endPositions) {
    return Collections.emptySet();
  }

  @Override
  public Collection<String> getImportsToAdd() {
    return Collections.emptyList();
  }

  @Override
  public Collection<String> getImportsToRemove() {
    return Collections.emptyList();
  }
}
