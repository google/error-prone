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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeInfo.DeclarationInfo;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "TopLevel",
  summary = "Source files should not contain multiple top-level class declarations",
  explanation =
      "The Google Java Style Guide §3.4.1 requires each source file to contain"
          + " \"exactly one top-level class\".",
  category = JDK,
  severity = WARNING,
  maturity = MATURE
)
public class MultipleTopLevelClasses extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTreeInfo info, VisitorState state) {
    if (info.typeDeclarations().size() <= 1) {
      // package-info.java files have zero top-level declarations, everything
      // else should have exactly one.
      return Description.NO_MATCH;
    }
    if (!info.packageName().isPresent()) {
      // Real code doesn't use the default package.
      return Description.NO_MATCH;
    }
    List<String> names = new ArrayList<>();
    for (DeclarationInfo member : info.typeDeclarations()) {
      switch (member.kind()) {
        case CLASS:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case ENUM:
          names.add(member.name());
          break;
        default:
          break;
      }
    }
    String message =
        String.format(
            "Expected at most one top-level class declaration, instead found: %s",
            Joiner.on(", ").join(names));
    return buildDescription(info.packageName().get()).setMessage(message).build();
  }
}
