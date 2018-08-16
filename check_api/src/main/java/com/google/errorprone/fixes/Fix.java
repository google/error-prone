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

package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.Collection;
import java.util.Set;

/**
 * Represents a source code transformation, usually used to fix a bug detected by error-prone.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public interface Fix {

  String toString(JCCompilationUnit compilationUnit);

  /**
   * A short description which can be attached to the Fix to differentiate multiple fixes provided
   * to the user.
   *
   * <p>Empty string generates the default description.
   */
  default String getShortDescription() {
    return "";
  }

  Set<Replacement> getReplacements(EndPosTable endPositions);

  Collection<String> getImportsToAdd();

  Collection<String> getImportsToRemove();

  boolean isEmpty();
}
