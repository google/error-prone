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
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;

@BugPattern(name = "JMockTestWithoutRunWithOrRuleAnnotation",
        summary = "JMock tests must have @RunWith class annotation or the mockery field declared as a JUnit rule",
        explanation = "If this is not done then all of your JMock tests will run and pass. However none of your assertions " +
                "will actually be evaluated so your tests will be producing false positive results.",
        category = Category.JMOCK, severity = ERROR, maturity = MATURE)
public class JMockTestWithoutRunWithOrRuleAnnotation extends BugChecker implements BugChecker.VariableTreeMatcher {

    private static final Matcher<VariableTree> JMOCK_MOCKERY_MATCHER = allOf(isSameType("org.jmock.Mockery"), isField());
    private static final Matcher<VariableTree> FIELD_A_RULE_ANNOTATION_MATHER = hasAnnotation("org.junit.Rule");
    private static final Matcher<Tree> CLASS_USING_JMOCK_RUNNER_MATCHER = enclosingClass(Matchers.<ClassTree>annotations(ChildMultiMatcher.MatchType.ALL, allOf(isType("org.junit.runner.RunWith"),
            hasArgumentWithValue("value", classLiteral(isSameType("org.jmock.integration.junit4.JMock"))))));

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (JMOCK_MOCKERY_MATCHER.matches(tree, state) && !(FIELD_A_RULE_ANNOTATION_MATHER.matches(tree, state) || CLASS_USING_JMOCK_RUNNER_MATCHER.matches(tree, state))) {
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
    }


}
