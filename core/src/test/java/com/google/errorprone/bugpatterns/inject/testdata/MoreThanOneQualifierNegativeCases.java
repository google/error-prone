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

package com.google.errorprone.bugpatterns.inject.testdata;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class MoreThanOneQualifierNegativeCases {
  /** A class in with no annotations on any of its members. */
  public class TestClass1 {
    private int n;

    public TestClass1() {}

    public void setN(int n) {}
  }

  /**
   * A class in which a single javax.inject.Qualifier annotation is on the class, on a constructor,
   * on a field, on a method, and on a method parameter.
   */
  @Foo
  public class TestClass2 {
    @Foo private int n;

    @Foo
    public TestClass2() {}

    @Foo
    public void setN(@Foo int n) {}
  }

  /**
   * A class in which a single com.google.inject.BindingAnnotation annotation is on the class, on a
   * constructor, on a field, on a method, and on a method parameter.
   */
  @Bar
  public class TestClass3 {
    @Bar private int n;

    @Bar
    public TestClass3() {}

    @Bar
    public void setN(@Bar int n) {}
  }

  @Qualifier
  @Retention(RUNTIME)
  public @interface Foo {}

  @BindingAnnotation
  @Retention(RUNTIME)
  public @interface Bar {}
}
