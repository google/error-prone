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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;

/**
 * Bug checker to detect usage of {@code extends throwable;}.
 */
@BugPattern(
        name = "ClassExtendsThrowable",
        summary = "Bad practise to extend throwable, extend Exception, Error, or RuntimeException instead.",
        severity = SeverityLevel.WARNING)
public class ClassExtendsThrowable extends BugChecker implements ClassTreeMatcher {


        @Override
        public Description matchClass(ClassTree tree, VisitorState state) {

                // Gets whatever the class is an extension of
                Tree extendsClause = tree.getExtendsClause();

                if (extendsClause == null) {
                        // Doesn't extend anything, can't possibly be a violation.
                        return Description.NO_MATCH;
                } else if (extendsClause.toString().equals("Throwable")) {
                        return buildDescription(tree).build();
                }
                return Description.NO_MATCH;
        }
}