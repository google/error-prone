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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JMockTestWithoutRunWithOrRuleAnnotationTest {

    private CompilationTestHelper compilationTestHelper;

    @Before
    public void setup() {
        compilationTestHelper =CompilationTestHelper.newInstance(new JMockTestWithoutRunWithOrRuleAnnotation(),
                getClass());
    }

    @Test
    public void testPositiveCase() throws Exception {
        compilationTestHelper.
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationPositiveCase1.java").
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationPositiveCase2.java").
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationPositiveCase3.java").
                doTest();
    }

    @Test
    public void testNegativeCase() throws Exception {
        compilationTestHelper.
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationNegativeCase1.java").
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationNegativeCase2.java").
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationNegativeCase3.java").
                addSourceFile("JMockTestWithoutRunWithOrRuleAnnotationNegativeCase4.java").
                doTest();
    }
}