/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "ClassName",
  summary = "The source file name should match the name of the top-level class it contains",
  explanation =
      "Google Java Style Guide § 2.1 states, \"The source file name consists of the"
          + " case-sensitive name of the top-level class it contains, plus the .java extension.\"",
  category = JDK,
  severity = ERROR,
  maturity = MATURE
)
public class ClassName extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (tree.getTypeDecls().isEmpty() || tree.getPackageName() == null) {
      return Description.NO_MATCH;
    }
    String filename = Files.getNameWithoutExtension(tree.getSourceFile().getName());
    List<String> names = new ArrayList<>();
    for (Tree member : tree.getTypeDecls()) {
      if (member instanceof ClassTree) {
        ClassTree classMember = (ClassTree) member;
        if (classMember.getSimpleName().toString().equals(filename)) {
          return Description.NO_MATCH;
        }
        if (classMember.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
          // If any of the top-level types are public, javac will complain
          // if the filename doesn't match. We don't want to double-report
          // the error.
          return Description.NO_MATCH;
        }
        names.add(classMember.getSimpleName().toString());
      }
    }
    String message = String.format(
        "Expected a class declaration named %s inside %s.java, instead found: %s",
        filename, filename, Joiner.on(", ").join(names));
    return buildDescription(tree.getPackageName()).setMessage(message).build();
  }
}
