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

package com.google.errorprone.util;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MoreAnnotations}Test */
@RunWith(JUnit4.class)
public final class MoreAnnotationsTest {

  abstract static class MoreAnnotationsTester extends BugChecker
      implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

    private Description process(Tree tree) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym == null) {
        return NO_MATCH;
      }
      if (sym.getKind() == ElementKind.ANNOTATION_TYPE) {
        return NO_MATCH;
      }
      String annos =
          getAnnotations(sym)
              .map(c -> c.type.asElement().getSimpleName().toString())
              .collect(joining(", "));
      if (annos.isEmpty()) {
        return NO_MATCH;
      }
      return buildDescription(tree).setMessage(annos).build();
    }

    protected abstract Stream<? extends Compound> getAnnotations(Symbol sym);

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return process(tree);
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return process(tree);
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return process(tree);
    }
  }

  @BugPattern(summary = "A test checker.", severity = ERROR)
  public static class GetDeclarationAndTypeAttributesTester extends MoreAnnotationsTester {
    @Override
    protected Stream<Compound> getAnnotations(Symbol sym) {
      return MoreAnnotations.getDeclarationAndTypeAttributes(sym);
    }
  }

  @BugPattern(summary = "A test checker.", severity = ERROR)
  public static class GetTopLevelTypeAttributesTester extends MoreAnnotationsTester {
    @Override
    protected Stream<TypeCompound> getAnnotations(Symbol sym) {
      return MoreAnnotations.getTopLevelTypeAttributes(sym);
    }
  }

  @Test
  public void getDeclarationAndTypeAttributesTest() {
    CompilationTestHelper.newInstance(GetDeclarationAndTypeAttributesTester.class, getClass())
        .addSourceLines(
            "Annos.java",
            """
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Target;
@Target(TYPE_USE)
@interface Other {}
@Target(TYPE_USE)
@interface TA {}
@Target({TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE_PARAMETER})
@interface DA {}
@Target({TYPE, TYPE_USE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE_PARAMETER})
@interface A {}
""")
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;
            // BUG: Diagnostic contains: DA, A
            @DA @A class Test<T extends @Other String> {
              // BUG: Diagnostic contains: DA, A, TA
              @TA @DA @A Test() {}
              // BUG: Diagnostic contains: DA, A, TA
              @TA @DA @A List<@Other String> field;
              {
                // BUG: Diagnostic contains: DA, A, TA
                @TA @DA @A List<@Other String> local;
              }
              // BUG: Diagnostic contains: DA, A, TA
              @TA @DA @A List<@Other String> f(
                  // BUG: Diagnostic contains: DA, A, TA
                  @TA @DA @A List<@Other String> param) {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getTopLevelTypeAttributesTest() {
    CompilationTestHelper.newInstance(GetTopLevelTypeAttributesTester.class, getClass())
        .addSourceLines(
            "Annos.java",
            """
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Target;
@Target(TYPE_USE)
@interface Other {}
@Target(TYPE_USE)
@interface TA {}
@Target({TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE_PARAMETER})
@interface DA {}
@Target({TYPE, TYPE_USE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE_PARAMETER})
@interface A {}
""")
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;
            @DA @A class Test<T extends @Other String> {
              // BUG: Diagnostic contains: TA, A
              @TA @DA @A Test() {}
              // BUG: Diagnostic contains: TA, A
              @TA @DA @A List<@Other String> field;
              {
                // BUG: Diagnostic contains: TA, A
                @TA @DA @A List<@Other String> local;
              }
              // BUG: Diagnostic contains: TA, A
              @TA @DA @A List<@Other String> f(
                  // BUG: Diagnostic contains: TA, A
                  @TA @DA @A List<@Other String> param) {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassesTest() {
    CompilationTestHelper.newInstance(GetTopLevelTypeAttributesTester.class, getClass())
        .addSourceLines(
            "Annos.java",
            """
            import static java.lang.annotation.ElementType.TYPE_USE;
            import java.lang.annotation.Target;
            @Target(TYPE_USE) @interface A {}
            @Target(TYPE_USE) @interface B {}
            @Target(TYPE_USE) @interface C {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;
            abstract class Outer {
              class Middle {
                class Inner {}
              }
              class MiddleStatic {
                class Inner {}
                class InnerStatic {}
              }
              // BUG: Diagnostic contains: C
              @A Outer . @B Middle . @C Inner x;
              // BUG: Diagnostic contains: B
              Outer . @A MiddleStatic . @B Inner y;
              // BUG: Diagnostic contains: A
              Outer . MiddleStatic . @A InnerStatic z;
              // BUG: Diagnostic contains: C
              abstract @A Outer . @B Middle . @C Inner f();
              // BUG: Diagnostic contains: B
              abstract Outer . @A MiddleStatic . @B Inner g();
              // BUG: Diagnostic contains: A
              abstract Outer . MiddleStatic . @A InnerStatic h();
            }
            """)
        .doTest();
  }
}
