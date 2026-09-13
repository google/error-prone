/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MoreAnnotations}. */
@RunWith(JUnit4.class)
public class MoreAnnotationsTest extends CompilerBasedAbstractTest {

  @Test
  public void annotationAccessors() {
    writeFile(
        "TestAnno.java",
        """
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface MyAnno {
          String strVal() default "default";
          boolean boolVal() default false;
          RetentionPolicy[] enumArr() default {};
          RetentionPolicy enumVal() default RetentionPolicy.SOURCE;
          String[] strArr() default {};
          Class<?>[] typeArr() default {};
        }

        @Retention(RetentionPolicy.SOURCE)
        @interface SourceAnno {}

        @Target({ElementType.TYPE_USE})
        @interface TypeUseAnno {}

        @interface DefaultRetentionAnno {}

        @MyAnno(
            strVal = "hello",
            boolVal = true,
            enumArr = {RetentionPolicy.RUNTIME},
            enumVal = RetentionPolicy.CLASS,
            strArr = {"a", "b"},
            typeArr = {String.class, Integer.class})
        public class TestAnno {
          @MyAnno(strVal = "method")
          public void testMethod() {}
        }
        """);

    AtomicBoolean verified = new AtomicBoolean(false);
    assertCompiles(
        new Scanner() {
          @Override
          public Void visitClass(ClassTree tree, VisitorState state) {
            ClassSymbol sym = ASTHelpers.getSymbol(tree);
            if (sym.getSimpleName().contentEquals("TestAnno")) {
              Optional<? extends AnnotationMirror> anno =
                  MoreAnnotations.getAnnotation(sym, "MyAnno");
              assertThat(anno).isPresent();
              AnnotationMirror compound = anno.get();

              assertThat(MoreAnnotations.getStringValue(compound, "strVal")).hasValue("hello");
              assertThat(MoreAnnotations.getBooleanValue(compound, "boolVal")).hasValue(true);
              Optional<VarSymbol> enumVal = MoreAnnotations.getEnumValue(compound, "enumVal");
              assertThat(enumVal.map(c -> c.getSimpleName().toString())).hasValue("CLASS");
              assertThat(MoreAnnotations.getEnumValue(sym, "MyAnno")).isEmpty();
              List<VarSymbol> enumArr = MoreAnnotations.getEnumValues(compound, "enumArr");
              assertThat(enumArr.stream().map(c -> c.getSimpleName().toString()))
                  .containsExactly("RUNTIME");
              assertThat(MoreAnnotations.getStrings(compound, "strArr"))
                  .containsExactly("a", "b")
                  .inOrder();
              assertThat(
                      MoreAnnotations.getTypes(compound, "typeArr").stream().map(Object::toString))
                  .containsExactly("java.lang.String", "java.lang.Integer")
                  .inOrder();

              assertThat(
                      MoreAnnotations.getValue(compound, "strVal")
                          .map(MoreAnnotations::asStrings)
                          .orElseGet(Stream::empty))
                  .containsExactly("hello");
              assertThat(
                      MoreAnnotations.getValue(compound, "strArr")
                          .map(MoreAnnotations::asStrings)
                          .orElseGet(Stream::empty))
                  .containsExactly("a", "b")
                  .inOrder();
              assertThat(
                      MoreAnnotations.asAnnotations(
                              (AnnotationValue) sym.getAnnotationMirrors().get(0))
                          .map(a -> MoreAnnotations.asElement(a).getSimpleName().toString()))
                  .containsExactly("MyAnno");

              assertThat(MoreAnnotations.getAnnotationWithSimpleName(sym, "MyAnno")).isPresent();
              assertThat(MoreAnnotations.getAnnotationWithSimpleName(sym, "NonExistent")).isEmpty();
              verified.set(true);
            }
            return super.visitClass(tree, state);
          }

          @Override
          public Void visitMethod(MethodTree tree, VisitorState state) {
            MethodSymbol sym = ASTHelpers.getSymbol(tree);
            if (sym.getSimpleName().contentEquals("testMethod")) {
              Optional<? extends AnnotationMirror> anno =
                  MoreAnnotations.getAnnotation(sym, "MyAnno");
              assertThat(anno).isPresent();
              assertThat(
                      MoreAnnotations.asElement(anno.get()).getSimpleName().contentEquals("MyAnno"))
                  .isTrue();
              assertThat(MoreAnnotations.getStringValue(anno.get(), "strVal")).hasValue("method");
            }
            return super.visitMethod(tree, state);
          }
        });
    assertThat(verified.get()).isTrue();
  }

  @Test
  public void targetAndRetentionHelpers() {
    writeFile(
        "MetaTest.java",
        """
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Target({ElementType.TYPE, ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        @interface TargetAndRuntimeAnno {}

        @Target({ElementType.TYPE_USE})
        @Retention(RetentionPolicy.SOURCE)
        @interface TypeUseAndSourceAnno {}

        @interface DefaultAnno {}

        public class MetaTest {}
        """);

    AtomicBoolean verified = new AtomicBoolean(false);
    assertCompiles(
        new Scanner() {
          @Override
          public Void visitClass(ClassTree tree, VisitorState state) {
            ClassSymbol sym = ASTHelpers.getSymbol(tree);
            if (sym.getSimpleName().contentEquals("TargetAndRuntimeAnno")) {
              assertThat(MoreAnnotations.getTargetElementTypes(sym)).containsExactly(TYPE, METHOD);
              assertThat(MoreAnnotations.isTypeAnnotation(sym)).isFalse();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, METHOD)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE_USE)).isFalse();
              assertThat(MoreAnnotations.getRetentionPolicy(sym))
                  .isEqualTo(RetentionPolicy.RUNTIME);
            } else if (sym.getSimpleName().contentEquals("TypeUseAndSourceAnno")) {
              assertThat(MoreAnnotations.getTargetElementTypes(sym)).containsExactly(TYPE_USE);
              assertThat(MoreAnnotations.isTypeAnnotation(sym)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE_USE)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE)).isFalse();
              assertThat(MoreAnnotations.getRetentionPolicy(sym)).isEqualTo(RetentionPolicy.SOURCE);
            } else if (sym.getSimpleName().contentEquals("DefaultAnno")) {
              assertThat(MoreAnnotations.getTargetElementTypes(sym)).isNull();
              assertThat(MoreAnnotations.isTypeAnnotation(sym)).isFalse();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, METHOD)).isTrue();
              assertThat(MoreAnnotations.targetsElement(sym, TYPE_USE)).isFalse();
              assertThat(MoreAnnotations.getRetentionPolicy(sym)).isEqualTo(RetentionPolicy.CLASS);
              verified.set(true);
            }
            return super.visitClass(tree, state);
          }
        });
    assertThat(verified.get()).isTrue();
  }
}
