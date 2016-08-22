/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

/**
 * Matches classes that have two or more constructors annotated with @Inject.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
  name = "MoreThanOneInjectableConstructor",
  summary =
      "This class has more than one @Inject-annotated constructor. Please remove the @Inject"
          + " annotation from all but one of them.",
  explanation =
      "Injection frameworks may use `@Inject` to determine how to construct an object"
          + " in the absence of other instructions. Annotating `@Inject` on a constructor tells"
          + " the injection framework to use that constructor. However, if multiple `@Inject`"
          + " constructors exist, injection frameworks can't reliably choose between them.",
  category = INJECT,
  severity = ERROR,
  maturity = MATURE,
  altNames = {"inject-constructors", "InjectMultipleAtInjectConstructors"}
)
public class MoreThanOneInjectableConstructor extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    boolean hasInjectConstructor = false;
    for (MethodTree constructor : ASTHelpers.getConstructors(classTree)) {
      if (hasInjectAnnotation().matches(constructor, state)) {
        if (hasInjectConstructor) {
          return describeMatch(classTree);
        }
        hasInjectConstructor = true;
      }
    }
    return Description.NO_MATCH;
  }

}
