/*
 * Copyright 2026 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster.testdata;

import com.google.common.collect.Lists;
import java.util.List;

/** Test data for {@code ShadowedImportTemplate}. */
public class ShadowedImportTemplateExample {
  static class Base {
    static class ArrayList<E> extends java.util.ArrayList<E> {}
  }

  static class InheritedMemberType extends Base {
    List<String> example() {
      return Lists.newArrayList();
    }
  }

  <ArrayList> List<String> methodTypeVariable() {
    return Lists.newArrayList();
  }

  static class ClassTypeVariableInStaticMethod<ArrayList> {
    static List<String> example() {
      return Lists.newArrayList();
    }
  }

  static class GenericOuter<T> {
    class ArrayList<E> extends java.util.ArrayList<E> {}

    static List<String> memberTypeInStaticContext() {
      return Lists.newArrayList();
    }
  }

  interface I1 {
    class ArrayList<E> extends java.util.ArrayList<E> {}
  }

  interface I2 {
    class ArrayList<E> extends java.util.ArrayList<E> {}
  }

  static class AmbiguousMemberType implements I1, I2 {
    List<String> example() {
      return Lists.newArrayList();
    }
  }

  List<String> unrelated() {
    return Lists.newLinkedList();
  }
}
