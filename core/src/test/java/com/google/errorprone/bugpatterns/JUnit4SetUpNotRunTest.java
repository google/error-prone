/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.common.io.ByteStreams;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.JUnit4;
import org.junit.runners.ParentRunner;

/** @author glorioso@google.com (Nick Glorioso) */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(JUnit4SetUpNotRun.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.addSourceFile("JUnit4SetUpNotRunPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCase_customBefore() throws Exception {
    compilationHelper.addSourceFile("JUnit4SetUpNotRunPositiveCaseCustomBefore.java").doTest();
  }

  @Test
  public void testNegativeCases() throws Exception {
    compilationHelper.addSourceFile("JUnit4SetUpNotRunNegativeCases.java").doTest();
  }

  public abstract static class SuperTest {
    @Before
    public void setUp() {}
  }

  @Test
  public void noBeforeOnClasspath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, RunWith.class);
      addClassToJar(jos, JUnit4.class);
      addClassToJar(jos, BlockJUnit4ClassRunner.class);
      addClassToJar(jos, ParentRunner.class);
      addClassToJar(jos, SuperTest.class);
      addClassToJar(jos, SuperTest.class.getEnclosingClass());
    }
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import " + SuperTest.class.getCanonicalName() + ";",
            "@RunWith(JUnit4.class)",
            "class Test extends SuperTest {",
            "  @Override public void setUp() {}",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }
}
