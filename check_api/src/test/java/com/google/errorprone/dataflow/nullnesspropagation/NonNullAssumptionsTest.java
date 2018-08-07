/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.ArbitraryInstances;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransfer.MemberName;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests to verify assumptions about specific JDK and other methods and fields built into {@link
 * NullnessPropagationTransfer} by running the referenced methods and reading the referenced fields
 * where possible/feasible.
 *
 * @author kmb@google.com (Kevin Bierhoff)
 */
// TODO(kmb): Add tests for methods assumed to return non-null values (for all inputs...)
@RunWith(JUnit4.class)
public class NonNullAssumptionsTest {

  @Test
  public void testClassesWithNonNullStaticFields() throws Exception {
    for (String classname : NullnessPropagationTransfer.CLASSES_WITH_NON_NULL_CONSTANTS) {
      int found = 0;
      Class<?> clazz = loadClass(classname);
      for (Field field : clazz.getDeclaredFields()) {
        if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
          ++found;
          field.setAccessible(true);
          assertThat(field.get(null)).named(field.toString()).isNotNull();
        }
      }
      assertWithMessage(classname).that(found).isGreaterThan(0);
    }
  }

  @Test
  public void testNullImpliesTrueParameters() throws Exception {
    for (MemberName member : NullnessPropagationTransfer.NULL_IMPLIES_TRUE_PARAMETERS.keySet()) {
      ImmutableSet<Integer> nullParameters =
          NullnessPropagationTransfer.NULL_IMPLIES_TRUE_PARAMETERS.get(member);
      assertWithMessage(member.clazz + "#" + member.member + "()")
          .that(nullParameters)
          .isNotEmpty();
      if (member.clazz.startsWith("android.")) {
        // Can't load Android SDK classes.
        continue;
      }
      int found = 0;
      for (Method method : loadClass(member.clazz).getMethods()) {
        if (!method.getName().equals(member.member)) {
          continue;
        }
        ++found;
        for (int nullParam : nullParameters) {
          // The following assertion would also fail if method returned something other than boolean
          assertThat(invokeWithSingleNullArgument(method, nullParam)).isEqualTo(Boolean.TRUE);
        }
      }
      assertWithMessage(member.clazz + "#" + member.member + "()").that(found).isGreaterThan(0);
    }
  }

  @Test
  public void testRequiredNonNullParameters() throws Exception {
    for (MemberName member : NullnessPropagationTransfer.REQUIRED_NON_NULL_PARAMETERS.keySet()) {
      ImmutableSet<Integer> nonNullParameters =
          NullnessPropagationTransfer.REQUIRED_NON_NULL_PARAMETERS.get(member);
      assertWithMessage(member.clazz + "#" + member.member + "()")
          .that(nonNullParameters)
          .isNotEmpty();
      int found = 0;
      for (Method method : loadClass(member.clazz).getMethods()) {
        if (!method.getName().equals(member.member)) {
          continue;
        }
        ++found;
        for (int nonNullParam : nonNullParameters) {
          try {
            invokeWithSingleNullArgument(method, nonNullParam);
            fail(
                "InvocationTargetException expected calling "
                    + method
                    + " with null parameter "
                    + nonNullParam);
          } catch (InvocationTargetException expected) {
          }
        }
      }
      assertWithMessage(member.clazz + "#" + member.member + "()").that(found).isGreaterThan(0);
    }
  }

  @Test
  public void testEqualsParameters() throws Exception {
    int found = 0;
    for (Method method : loadClass("java.lang.Object").getMethods()) {
      if (!method.getName().equals("equals")) {
        continue;
      }
      found++;
      assertThat(invokeWithSingleNullArgument(method, 0)).isEqualTo(Boolean.FALSE);
    }
    assertWithMessage("equals()").that(found).isGreaterThan(0);
  }

  private static Object invokeWithSingleNullArgument(Method method, int nullParam)
      throws IllegalAccessException, InvocationTargetException {
    Class<?>[] params = method.getParameterTypes();
    int nonNullIndex = nullParam < 0 ? params.length + nullParam : nullParam;
    assertWithMessage(method.toString()).that(nonNullIndex).isLessThan(params.length);

    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; ++i) {
      if (i != nonNullIndex) {
        args[i] = ArbitraryInstances.get(params[i]);
      }
    }
    Object receiver =
        Modifier.isStatic(method.getModifiers())
            ? null
            : ArbitraryInstances.get(method.getDeclaringClass());
    method.setAccessible(true);
    return method.invoke(receiver, args);
  }

  private static Class<?> loadClass(String classname) throws Exception {
    return Thread.currentThread().getContextClassLoader().loadClass(classname);
  }
}
