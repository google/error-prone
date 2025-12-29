/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author ronshapiro@google.com (Ron Shapiro)
 */
@RunWith(JUnit4.class)
public class AutoFactoryAtInjectTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AutoFactoryAtInject.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "AutoFactoryAtInjectPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.auto.factory.AutoFactory;
            import javax.inject.Inject;

            class AssistedInjectAndInjectOnSameConstructorPositiveCases {

              @AutoFactory
              static class HasAutoFactoryOnClass {
                // BUG: Diagnostic contains: remove
                @Inject
                HasAutoFactoryOnClass() {}
              }

              @AutoFactory
              static class UsesGuiceInject {
                // BUG: Diagnostic contains: remove
                @com.google.inject.Inject
                UsesGuiceInject() {}
              }

              static class HasAutoFactoryOnConstructor {
                // BUG: Diagnostic contains: remove
                @Inject
                @AutoFactory
                HasAutoFactoryOnConstructor() {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "AutoFactoryAtInjectNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.auto.factory.AutoFactory;
            import javax.inject.Inject;

            class AutoFactoryAtInjectNegativeCases {

              @AutoFactory
              static class AtInjectOnInnerType {
                static class InnerType {
                  @Inject
                  InnerType() {}
                }
              }

              static class AutoFactoryOnInnerType {
                @Inject
                AutoFactoryOnInnerType() {}

                @AutoFactory
                static class InnerType {}
              }

              static class OnDifferentConstructors {
                @Inject
                OnDifferentConstructors(String string) {}

                @AutoFactory
                OnDifferentConstructors(Object object) {}
              }
            }
            """)
        .doTest();
  }
}
