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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.isAmbiguousJUnitVersion;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;

/** @author mwacker@google.com (Mike Wacker) */
@BugPattern(
    name = "JUnitAmbiguousTestClass",
    summary =
        "Test class inherits from JUnit 3's TestCase but has JUnit 4 @Test or @RunWith"
            + " annotations.",
    severity = WARNING)
public class JUnitAmbiguousTestClass extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return isAmbiguousJUnitVersion.matches(classTree, state) ? describeMatch(classTree) : NO_MATCH;
  }
}
