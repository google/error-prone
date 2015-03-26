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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isType;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
@BugPattern(name = "NullablePrimitive",
            summary = "@Nullable should not be used for primitive types.",
            explanation = "Primitives can never be null, annotating a primitive with @Nullable"
                + " may be hinting at an intent that cannot be fulfilled.",
            category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class NullablePrimitive extends BugChecker implements BugChecker.AnnotationTreeMatcher {

    private static final String NULLABLE_ANNOTATION = "javax.annotation.Nullable";

    private Matcher<AnnotationTree> nullableAnnotationMatcher = isType(NULLABLE_ANNOTATION);

    @Override
    public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
        if (nullableAnnotationMatcher.matches(tree, state)) {
            if (isPrimitiveType().matches(getAnnotatedNode(state), state)) {
                return describeMatch(tree, SuggestedFix.delete(tree));
            }
        }
        return Description.NO_MATCH;
    }

    private static Tree getAnnotatedNode(VisitorState state) {
        return state.getPath().getParentPath().getParentPath().getLeaf();
    }

}
