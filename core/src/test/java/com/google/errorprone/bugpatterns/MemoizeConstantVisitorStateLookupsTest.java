/*
 * Copyright 2020 The Error Prone Authors.
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

/** Tests for {@link com.google.errorprone.bugpatterns.MemoizeConstantVisitorStateLookups}. */
@RunWith(JUnit4.class)
public class MemoizeConstantVisitorStateLookupsTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(MemoizeConstantVisitorStateLookups.class, getClass())
          .addModules(
              "jdk.compiler/com.sun.tools.javac.util", "jdk.compiler/com.sun.tools.javac.code");
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
              MemoizeConstantVisitorStateLookups.class, getClass())
          .addModules(
              "jdk.compiler/com.sun.tools.javac.util", "jdk.compiler/com.sun.tools.javac.code");

  @Test
  public void replaceSingleUsage() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Name me = state.getName("Test");
                Name init = state.getName("<init>");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.util.Name;

class Test {
  public Test(VisitorState state) {
    Name me = TEST.get(state);
    Name init = _INIT_.get(state);
  }

  private static final Supplier<Name> TEST = VisitorState.memoize(state -> state.getName("Test"));

  private static final Supplier<Name> _INIT_ =
      VisitorState.memoize(state -> state.getName("<init>"));
}
""")
        .doTest();
  }

  @Test
  public void prefersExistingStringConstant() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Type;

            class Test {
              private static final String MAP = "java.util.Map";

              public Test(VisitorState state) {
                Type map = state.getTypeFromString(MAP);
                Type map2 = state.getTypeFromString("java.util.Map");
                Symbol mapSym = state.getSymbolFromString("java.util.Map");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.ClassName;
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Type;

            class Test {
              private static final String MAP = "java.util.Map";

              public Test(VisitorState state) {
                Type map = JAVA_UTIL_MAP.type(state);
                Type map2 = JAVA_UTIL_MAP.type(state);
                Symbol mapSym = JAVA_UTIL_MAP.symbol(state);
              }

              private static final ClassName JAVA_UTIL_MAP = ClassName.of(MAP);
            }
            """)
        .doTest();
  }

  @Test
  public void replaceConflictingValues() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Name className = state.getName("java.lang.Class");
                Type classType = state.getTypeFromString("java.lang.Class");
                Name lookupAgain = state.getName("java.lang.Class");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.ClassName;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.suppliers.Supplier;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Name className = JAVA_LANG_CLASS_NAME.get(state);
                Type classType = JAVA_LANG_CLASS_TYPE.type(state);
                Name lookupAgain = JAVA_LANG_CLASS_NAME.get(state);
              }

              private static final ClassName JAVA_LANG_CLASS_TYPE = ClassName.of("java.lang.Class");

              private static final Supplier<Name> JAVA_LANG_CLASS_NAME =
                  VisitorState.memoize(state -> state.getName("java.lang.Class"));
            }
            """)
        .doTest();
  }

  @Test
  public void findingOnLookup() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                // BUG: Diagnostic contains:
                Name className = state.getName("java.lang.Class");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_doesntMemoizeTwice() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
"""
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.util.Name;

class Test {
  private static final Supplier<ImmutableSet<Name>> ALLOWED_NAMES =
      VisitorState.memoize(state -> ImmutableSet.of(state.getName("foo"), state.getName("bar")));
}
""")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSuppressWarnings() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.util.Name;

            class Test {
              @SuppressWarnings("MemoizeConstantVisitorStateLookups")
              public Test(VisitorState state) {
                Name className = state.getName("java.lang.Class");
              }

              @SuppressWarnings("MemoizeConstantVisitorStateLookups")
              public void testMethod(VisitorState state) {
                Name className = state.getName("java.lang.Class");
              }

              @SuppressWarnings("MemoizeConstantVisitorStateLookups")
              class InnerClass {
                void innerMethod(VisitorState state) {
                  Name className = state.getName("java.lang.Class");
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassLookups_deduplicatesCanonicalAndBinaryNames() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Type entryType = state.getTypeFromString("java.util.Map.Entry");
                Symbol entrySym = state.getSymbolFromString("java.util.Map$Entry");
                Name entryBinary = state.binaryNameFromClassname("java.util.Map.Entry");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.ClassName;
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Type entryType = JAVA_UTIL_MAP_ENTRY.type(state);
                Symbol entrySym = JAVA_UTIL_MAP_ENTRY.symbol(state);
                Name entryBinary = JAVA_UTIL_MAP_ENTRY.binaryName(state);
              }

              private static final ClassName JAVA_UTIL_MAP_ENTRY = ClassName.of("java.util.Map.Entry");
            }
            """)
        .doTest();
  }

  @Test
  public void existingFieldNameCollision_appendsTypeSuffix() {
    refactoringTestHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static final String JAVA_UTIL_SET = "java.util.Set";
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Type;

            class Test extends Super {
              private static final String JAVA_UTIL_LIST = "unrelated";
              private static final String JAVA_UTIL_MAP = "java.util.Map";
              private static final String JAVA_UTIL_MAP_TYPE = "unrelated";

              public Test(VisitorState state) {
                Type list = state.getTypeFromString("java.util.List");
                Type set = state.getTypeFromString(JAVA_UTIL_SET);
                Type map = state.getTypeFromString(JAVA_UTIL_MAP);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.ClassName;
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Type;

            class Test extends Super {
              private static final String JAVA_UTIL_LIST = "unrelated";
              private static final String JAVA_UTIL_MAP = "java.util.Map";
              private static final String JAVA_UTIL_MAP_TYPE = "unrelated";

              public Test(VisitorState state) {
                Type list = JAVA_UTIL_LIST_TYPE.type(state);
                Type set = JAVA_UTIL_SET_TYPE.type(state);
                Type map = JAVA_UTIL_MAP_TYPE_2.type(state);
              }

              private static final ClassName JAVA_UTIL_LIST_TYPE = ClassName.of("java.util.List");

              private static final ClassName JAVA_UTIL_MAP_TYPE_2 = ClassName.of(JAVA_UTIL_MAP);

              private static final ClassName JAVA_UTIL_SET_TYPE = ClassName.of(JAVA_UTIL_SET);
            }
            """)
        .doTest();
  }

  @Test
  public void getNameAndSymbolWithSameConstant_appendsNameAndTypeSuffixes() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.util.Name;

            class Test {
              private static final String ENTRY = "java.util.Map.Entry";

              public Test(VisitorState state) {
                Name entryName = state.getName(ENTRY);
                Symbol entrySym = state.getSymbolFromString(ENTRY);
                Symbol entryBinarySym = state.getSymbolFromString("java.util.Map$Entry");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.ClassName;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.suppliers.Supplier;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.util.Name;

            class Test {
              private static final String ENTRY = "java.util.Map.Entry";

              public Test(VisitorState state) {
                Name entryName = JAVA_UTIL_MAP_ENTRY_NAME.get(state);
                Symbol entrySym = JAVA_UTIL_MAP_ENTRY_TYPE.symbol(state);
                Symbol entryBinarySym = JAVA_UTIL_MAP_ENTRY_TYPE.symbol(state);
              }

              private static final ClassName JAVA_UTIL_MAP_ENTRY_TYPE = ClassName.of(ENTRY);

              private static final Supplier<Name> JAVA_UTIL_MAP_ENTRY_NAME =
                  VisitorState.memoize(state -> state.getName(ENTRY));
            }
            """)
        .doTest();
  }

  @Test
  public void primitivesAndInvalidClassNames_notMemoized() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.VisitorState;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.util.Name;

            class Test {
              public Test(VisitorState state) {
                Type intType = state.getTypeFromString("int");
                Type voidType = state.getTypeFromString("void");
                Type arrayType = state.getTypeFromString("java.lang.String[]");
                Type genericType = state.getTypeFromString("java.util.List<String>");
                Symbol emptySym = state.getSymbolFromString("");
                Name trailingDot = state.binaryNameFromClassname("java.lang.");
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
