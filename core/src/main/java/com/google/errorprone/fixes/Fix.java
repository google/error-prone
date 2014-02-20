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
import java.util.Set;

/**
 * Represents a source code transformation, usually used to fix a bug detected by error-prone.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public interface Fix {

  /**
   * An immutable value to use when there is no fix.
   */
  public static final Fix NO_FIX = new NoFix();

  public String toString(JCCompilationUnit compilationUnit);

  public Set<Replacement> getReplacements(ErrorProneEndPosMap endPositions);

  public Collection<String> getImportsToAdd();

  public Collection<String> getImportsToRemove();

}
