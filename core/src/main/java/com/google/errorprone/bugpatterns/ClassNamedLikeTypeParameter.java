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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;

/** @author glorioso@google.com */
@BugPattern(
    name = "ClassNamedLikeTypeParameter",
    summary = "This class's name looks like a Type Parameter.",
    category = JDK,
    severity = SUGGESTION,
    tags = StandardTags.STYLE)
public class ClassNamedLikeTypeParameter extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    // Here, if a class is named like a Type Parameter, it's a bad thing.
    return TypeParameterNamingClassification.classify(tree.getSimpleName().toString()).isValidName()
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }
}
