/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import javax.inject.Inject;

/** A collection of annotations to treat as if they were annotated with {@code @Keep}. */
public final class WellKnownKeep {

  /**
   * Annotations that exempt variables from being considered unused.
   *
   * <p>Try to avoid adding more annotations here. Annotating these annotations with {@code @Keep}
   * has the same effect; this list is chiefly for third-party annotations which cannot be
   * annotated.
   */
  private static final ImmutableSet<String> EXEMPTING_VARIABLE_ANNOTATIONS =
      ImmutableSet.of(
          "jakarta.persistence.Basic",
          "jakarta.persistence.Column",
          "jakarta.persistence.Id",
          "jakarta.persistence.Version",
          "jakarta.xml.bind.annotation.XmlElement",
          "javax.persistence.Basic",
          "javax.persistence.Column",
          "javax.persistence.Id",
          "javax.persistence.Version",
          "javax.xml.bind.annotation.XmlElement",
          "net.starlark.java.annot.StarlarkBuiltin",
          "org.junit.Rule",
          "org.junit.jupiter.api.extension.RegisterExtension",
          "org.openqa.selenium.support.FindAll",
          "org.openqa.selenium.support.FindBy",
          "org.openqa.selenium.support.FindBys",
          "org.apache.beam.sdk.transforms.DoFn.TimerId",
          "org.apache.beam.sdk.transforms.DoFn.StateId",
          "org.springframework.boot.test.mock.mockito.MockBean");

  /**
   * Annotations that exempt methods from being considered unused.
   *
   * <p>Try to avoid adding more annotations here. Annotating these annotations with {@code @Keep}
   * has the same effect; this list is chiefly for third-party annotations which cannot be
   * annotated.
   */
  private static final ImmutableSet<String> EXEMPTING_METHOD_ANNOTATIONS =
      ImmutableSet.of(
          "android.webkit.JavascriptInterface",
          "com.fasterxml.jackson.annotation.JsonCreator",
          "com.fasterxml.jackson.annotation.JsonProperty",
          "com.fasterxml.jackson.annotation.JsonSetter",
          "com.fasterxml.jackson.annotation.JsonValue",
          "com.google.acai.AfterTest",
          "com.google.acai.BeforeSuite",
          "com.google.acai.BeforeTest",
          "com.google.caliper.Benchmark",
          "com.google.common.eventbus.Subscribe",
          "com.google.inject.Provides",
          "com.google.inject.Inject",
          "com.google.inject.multibindings.ProvidesIntoMap",
          "com.google.inject.multibindings.ProvidesIntoSet",
          "com.google.inject.throwingproviders.CheckedProvides",
          "com.tngtech.java.junit.dataprovider.DataProvider",
          "jakarta.annotation.PreDestroy",
          "jakarta.annotation.PostConstruct",
          "jakarta.inject.Inject",
          "jakarta.persistence.PostLoad",
          "jakarta.persistence.PostPersist",
          "jakarta.persistence.PostRemove",
          "jakarta.persistence.PostUpdate",
          "jakarta.persistence.PrePersist",
          "jakarta.persistence.PreRemove",
          "jakarta.persistence.PreUpdate",
          "jakarta.validation.constraints.AssertFalse",
          "jakarta.validation.constraints.AssertTrue",
          "javax.annotation.PreDestroy",
          "javax.annotation.PostConstruct",
          "javax.inject.Inject",
          "javax.persistence.PostLoad",
          "javax.persistence.PostPersist",
          "javax.persistence.PostRemove",
          "javax.persistence.PostUpdate",
          "javax.persistence.PrePersist",
          "javax.persistence.PreRemove",
          "javax.persistence.PreUpdate",
          "javax.validation.constraints.AssertFalse",
          "javax.validation.constraints.AssertTrue",
          "net.bytebuddy.asm.Advice.OnMethodEnter",
          "net.bytebuddy.asm.Advice.OnMethodExit",
          "org.apache.beam.sdk.transforms.DoFn.FinishBundle",
          "org.apache.beam.sdk.transforms.DoFn.ProcessElement",
          "org.apache.beam.sdk.transforms.DoFn.StartBundle",
          "org.aspectj.lang.annotation.Pointcut",
          "org.aspectj.lang.annotation.After",
          "org.aspectj.lang.annotation.Before",
          "org.springframework.context.annotation.Bean",
          "org.testng.annotations.AfterClass",
          "org.testng.annotations.AfterMethod",
          "org.testng.annotations.BeforeClass",
          "org.testng.annotations.BeforeMethod",
          "org.testng.annotations.DataProvider",
          "org.junit.jupiter.api.BeforeAll",
          "org.junit.jupiter.api.AfterAll",
          "org.junit.jupiter.api.AfterEach",
          "org.junit.jupiter.api.BeforeEach",
          "org.junit.jupiter.api.RepeatedTest",
          "org.junit.jupiter.api.Test",
          "org.junit.jupiter.params.ParameterizedTest");

  private final ImmutableSet<String> exemptingVariableAnnotations;
  private final ImmutableSet<String> exemptingMethodAnnotations;

  @Inject
  WellKnownKeep(ErrorProneFlags flags) {
    this.exemptingVariableAnnotations = EXEMPTING_VARIABLE_ANNOTATIONS;
    this.exemptingMethodAnnotations =
        ImmutableSet.<String>builder()
            .addAll(EXEMPTING_METHOD_ANNOTATIONS)
            .addAll(flags.getSetOrEmpty("UnusedMethod:ExemptingMethodAnnotations"))
            .build();
  }

  public final boolean shouldKeep(VariableTree tree) {
    return shouldKeep(tree, tree.getModifiers(), exemptingVariableAnnotations);
  }

  public final boolean shouldKeep(MethodTree tree) {
    return shouldKeep(tree, tree.getModifiers(), exemptingMethodAnnotations);
  }

  public final boolean shouldKeep(ClassTree tree) {
    return shouldKeep(tree, tree.getModifiers(), ImmutableSet.of());
  }

  public final boolean shouldKeep(Tree tree) {
    return firstNonNull(
        new SimpleTreeVisitor<Boolean, Void>() {
          @Override
          public Boolean visitVariable(VariableTree tree, Void unused) {
            return shouldKeep(tree);
          }

          @Override
          public Boolean visitMethod(MethodTree tree, Void unused) {
            return shouldKeep(tree);
          }

          @Override
          public Boolean visitClass(ClassTree tree, Void unused) {
            return shouldKeep(tree);
          }
        }.visit(tree, null),
        false);
  }

  private final boolean shouldKeep(
      Tree tree, ModifiersTree modifiers, ImmutableSet<String> exemptingAnnotations) {
    if (ASTHelpers.shouldKeep(tree)) {
      return true;
    }
    if (exemptedByAnnotation(modifiers.getAnnotations(), exemptingAnnotations)) {
      return true;
    }
    return false;
  }

  /**
   * Looks at the list of {@code annotations} and see if there is any annotation which exists {@code
   * exemptingAnnotations}.
   */
  private static boolean exemptedByAnnotation(
      List<? extends AnnotationTree> annotations, ImmutableSet<String> exemptingAnnotations) {
    for (AnnotationTree annotation : annotations) {
      Type annotationType = ASTHelpers.getType(annotation);
      if (annotationType == null) {
        continue;
      }
      TypeSymbol tsym = annotationType.tsym;
      if (exemptingAnnotations.contains(tsym.getQualifiedName().toString())) {
        return true;
      }
    }
    return false;
  }
}
