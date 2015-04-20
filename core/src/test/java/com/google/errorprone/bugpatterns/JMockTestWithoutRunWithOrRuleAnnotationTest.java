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
    public void setup()
    {
        compilationTestHelper = CompilationTestHelper.newInstance(new JMockTestWithoutRunWithOrRuleAnnotation());
    }

    @Test
    public void testPositiveCase() throws Exception
    {
        compilationTestHelper.assertCompileFailsWithMessages(compilationTestHelper.fileManager().
                sources(getClass(),
                        "JMockTestWithoutRunWithOrRuleAnnotationPositiveCase1.java",
                        "JMockTestWithoutRunWithOrRuleAnnotationPositiveCase2.java"));
    }

    @Test
    public void testNegativeCase() throws Exception
    {
        compilationTestHelper.assertCompileSucceedsWithMessages(compilationTestHelper.fileManager()
                .sources(getClass(),
                        "JMockTestWithoutRunWithOrRuleAnnotationNegativeCase1.java",
                        "JMockTestWithoutRunWithOrRuleAnnotationNegativeCase2.java",
                        "JMockTestWithoutRunWithOrRuleAnnotationNegativeCase3.java",
                        "JMockTestWithoutRunWithOrRuleAnnotationNegativeCase4.java",
                        "JMockTestWithoutRunWithOrRuleAnnotationNegativeCase5.java"));
    }
}