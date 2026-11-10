/*
 * Copyright 2026 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;

/// [BootclasspathShadowing]Test
public class BootclasspathShadowingTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(BootclasspathShadowing.class, getClass());

  private static final ImmutableList<String> JAVA8_JAVACOPTS =
      ImmutableList.of("-source", "8", "-target", "8");

  @Test
  public void positive_javaxCryptoCipher() {
    testHelper
        .addSourceLines(
            "javax/crypto/Cipher.java",
            """
            package javax.crypto;

            // BUG: Diagnostic contains: shadows a class on the compilation bootclasspath
            public class Cipher {}
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }

  @Test
  public void positive_javaUtilArrayList() {
    testHelper
        .addSourceLines(
            "java/util/ArrayList.java",
            """
            package java.util;

            // BUG: Diagnostic contains: shadows a class on the compilation bootclasspath
            public class ArrayList<E> {}
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }

  @Test
  public void positive_javaLangString() {
    testHelper
        .addSourceLines(
            "java/lang/String.java",
            """
            package java.lang;

            // BUG: Diagnostic contains: shadows a class on the compilation bootclasspath
            public class String {}
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }

  @Test
  public void positive_systemModules() {
    testHelper
        .addSourceLines(
            "sun/nio/ch/DirectBuffer.java",
            """
            package sun.nio.ch;

            // BUG: Diagnostic contains: shadows a class on the compilation bootclasspath
            public interface DirectBuffer {}
            """)
        .doTest();
  }

  @Test
  public void positive_otherSystemModule() {
    testHelper
        .addSourceLines(
            "sun/rmi/runtime/Log.java",
            """
            package sun.rmi.runtime;

            // BUG: Diagnostic contains: shadows a class on the compilation bootclasspath
            public abstract class Log {}
            """)
        .doTest();
  }

  @Test
  public void negative_systemModulesNonExistentClass() {
    testHelper
        .addSourceLines(
            "sun/nio/ch/NotARealPlatformClass.java",
            """
            package sun.nio.ch;

            public class NotARealPlatformClass {}
            """)
        .doTest();
  }

  @Test
  public void negative_normalUserClass() {
    testHelper
        .addSourceLines(
            "com/example/Cipher.java",
            """
            package com.example;

            public class Cipher {
              public static class String {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative_nestedClassInPlatformPackage() {
    testHelper
        .addSourceLines(
            "javax/crypto/NotARealPlatformClass.java",
            """
            package javax.crypto;

            public class NotARealPlatformClass {
              public static class Cipher {}
            }
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }

  @Test
  public void negative_packageInfo() {
    testHelper
        .addSourceLines(
            "javax/crypto/package-info.java",
            """
            package javax.crypto;
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }

  @Test
  public void suppressed() {
    testHelper
        .addSourceLines(
            "javax/crypto/Cipher.java",
            """
            package javax.crypto;

            @SuppressWarnings("BootclasspathShadowing")
            public class Cipher {}
            """)
        .setArgs(JAVA8_JAVACOPTS)
        .doTest();
  }
}
