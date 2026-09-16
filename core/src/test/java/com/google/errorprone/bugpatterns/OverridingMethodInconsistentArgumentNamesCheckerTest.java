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

import static org.junit.Assert.assertEquals;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.errorprone.CompilationTestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OverridingMethodInconsistentArgumentNamesCheckerTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(
          OverridingMethodInconsistentArgumentNamesChecker.class, getClass());

  @Test
  public void positiveSwap() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              void m(int p1, int p2) {}
            }
            """)
        .addSourceLines(
            "B.java",
            """
            class B extends A {
              @Override
              // BUG: Diagnostic contains: A consistent order would be: m(p1, p2)
              void m(int p2, int p1) {}
            }
            """)
        .doTest();
  }

  @Test
  public void positivePermutation() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              void m(int p1, int p2, int p3) {}
            }
            """)
        .addSourceLines(
            "B.java",
            """
            class B extends A {
              @Override
              // BUG: Diagnostic contains: A consistent order would be: m(p1, p2, p3)
              void m(int p3, int p1, int p2) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              void m(int p1, int p2) {}
            }
            """)
        .addSourceLines(
            "B.java",
            """
            class B extends A {
              @Override
              void m(int p1, int p2) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative2() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              void m(int p1, int p2) {}
            }
            """)
        .addSourceLines(
            "B.java",
            """
            class B extends A {
              @Override
              void m(int p1, int p3) {}
            }
            """)
        .doTest();
  }

  @Test
  public void duplicateParameterNamesInSuperMethod() throws Exception {
    Path dir = Files.createTempDirectory("ep-dup-param-names");
    try {
      Path source = dir.resolve("A.java");
      Files.write(source, List.of("class A {", "  void m(int p1, int p2) {}", "}"));
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      assertEquals(
          "compiling the superclass failed",
          0,
          compiler.run(
              null,
              null,
              null,
              "--release",
              "21",
              "-parameters",
              "-d",
              dir.toString(),
              source.toString()));
      // Obfuscators sometimes rewrite every parameter of a method to the same name. javac is fine
      // with that when reading a class file, so emulate it by renaming the second parameter.
      Path classFile = dir.resolve("A.class");
      Files.write(classFile, mangleParameterNames(Files.readAllBytes(classFile)));

      testHelper
          .addSourceLines(
              "B.java",
              """
              class B extends A {
                @Override
                void m(int p1, int p2) {}
              }
              """)
          .setArgs("-classpath", dir.toString())
          .doTest();
    } finally {
      MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE);
    }
  }

  private static byte[] mangleParameterNames(byte[] classFile) {
    // Rewrites the constant-pool entry for "p2" to "p1" (same length), so both parameters of A.m
    // end up with the same name in the MethodParameters attribute.
    int offset = 8; // magic, minor version, major version
    int constantPoolCount = readU2(classFile, offset);
    offset += 2;
    for (int i = 1; i < constantPoolCount; i++) {
      int tag = classFile[offset++] & 0xff;
      switch (tag) {
        case 1: // Utf8
          int length = readU2(classFile, offset);
          offset += 2;
          if (length == 2 && classFile[offset] == 'p' && classFile[offset + 1] == '2') {
            classFile[offset + 1] = '1';
          }
          offset += length;
          break;
        case 3: // Integer
        case 4: // Float
          offset += 4;
          break;
        case 5: // Long
        case 6: // Double
          offset += 8;
          i++;
          break;
        case 7: // Class
        case 8: // String
        case 16: // MethodType
        case 19: // Module
        case 20: // Package
          offset += 2;
          break;
        case 9: // Fieldref
        case 10: // Methodref
        case 11: // InterfaceMethodref
        case 12: // NameAndType
        case 17: // Dynamic
        case 18: // InvokeDynamic
          offset += 4;
          break;
        case 15: // MethodHandle
          offset += 3;
          break;
        default:
          throw new AssertionError("Unexpected constant pool tag: " + tag);
      }
    }
    return classFile;
  }

  private static int readU2(byte[] bytes, int offset) {
    return ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
  }
}
