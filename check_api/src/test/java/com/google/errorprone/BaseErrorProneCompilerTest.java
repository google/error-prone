/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone;

import java.util.List;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseErrorProneCompilerTest {
  @Captor private ArgumentCaptor<String[]> argsCaptor;
  @Captor private ArgumentCaptor<List<JavaFileObject>> javaFileObjectsCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void parseSourcesFromCommandLine() {
    BaseErrorProneCompiler baseErrorProneCompiler = mock(BaseErrorProneCompiler.class);
    when(baseErrorProneCompiler.run(any(String[].class))).thenCallRealMethod();

    baseErrorProneCompiler.run(new String[]{
        "-d", "classes",
        "-source", "8",
        "-target", "8",
        "-classpath", "/tmp/a.jar:/tmp/b.jar",
        "-Xep:StringSplitter:OFF",
        "com/example/HelloWorld.java"});

    verify(baseErrorProneCompiler).run(argsCaptor.capture(),
        any(JavaFileManager.class),
        javaFileObjectsCaptor.capture(),
        any());

    String[] capturedArgs = argsCaptor.<String[]> getValue();
    assertEquals(9, capturedArgs.length);
    assertEquals("-d", capturedArgs[0]);

    List<JavaFileObject> capturedJavaFileObjects = javaFileObjectsCaptor.<List<JavaFileObject>>getValue();
    assertEquals(1, capturedJavaFileObjects.size());
    assertTrue(capturedJavaFileObjects.get(0).getName().endsWith("HelloWorld.java"));
  }

  @Test
  public void parseOutputDirectoryEndsInJava() {
    BaseErrorProneCompiler baseErrorProneCompiler = mock(BaseErrorProneCompiler.class);
    when(baseErrorProneCompiler.run(any(String[].class))).thenCallRealMethod();

    baseErrorProneCompiler.run(new String[]{
        "-d", "classes.java",
        "-classpath", "/tmp/a.jar:/tmp/b.jar",
        "com/example/HelloWorld.java"});

    verify(baseErrorProneCompiler).run(argsCaptor.capture(),
        any(JavaFileManager.class),
        javaFileObjectsCaptor.capture(),
        any());

    String[] capturedArgs = argsCaptor.<String[]> getValue();
    assertEquals(4, capturedArgs.length);

    List<JavaFileObject> capturedJavaFileObjects = javaFileObjectsCaptor.<List<JavaFileObject>>getValue();
    assertEquals(1, capturedJavaFileObjects.size());
    assertTrue(capturedJavaFileObjects.get(0).getName().endsWith("HelloWorld.java"));
  }
}
