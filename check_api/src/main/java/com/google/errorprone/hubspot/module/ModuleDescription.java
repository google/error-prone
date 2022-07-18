/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.hubspot.module;

import com.google.common.base.Preconditions;
import com.google.errorprone.matchers.Description;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

// Descriptions returned by onModuleFinished need to include information
// about the compilation unit that triggered the description.
public class ModuleDescription {
  public static final ModuleDescription NO_MATCH = new ModuleDescription(Description.NO_MATCH, null);

  public static ModuleDescription of(Description description, JCCompilationUnit compilationUnit) {
    if (description == null || description == Description.NO_MATCH)  {
      return NO_MATCH;
    }

    Preconditions.checkArgument(compilationUnit != null, "Must provide a compilation unit");

    return new ModuleDescription(description, compilationUnit);
  }

  public static ModuleDescription of(Description description, TreePath problemTreePath) {
    return of(description, (JCCompilationUnit) problemTreePath.getCompilationUnit());
  }

  private final Description description;
  private final JCCompilationUnit compilationUnit;

  private ModuleDescription(Description description, JCCompilationUnit compilationUnit) {
    this.description = description;
    this.compilationUnit = compilationUnit;
  }

  public Description getDescription() {
    return description;
  }

  public JCCompilationUnit getCompilationUnit() {
    return compilationUnit;
  }
}
