/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link MultibindsInsteadOfMultibindings}. */
@RunWith(Parameterized.class)
public class MultibindsInsteadOfMultibindingsTest {

  @Parameters(name = "{0}, imported={2}")
  public static Iterable<Object[]> parameters() {
    return ImmutableList.copyOf(
        new Object[][] {
          {"dagger.Module", "dagger.Provides", false},
          {"dagger.Module", "dagger.Provides", true},
          {"dagger.producers.ProducerModule", "dagger.producers.Produces", false},
        });
  }

  private final String providesOrProduces;
  private final boolean importModuleAnnotations;
  private final String moduleAnnotation;
  private final String moduleAnnotationImport;
  private BugCheckerRefactoringTestHelper refactoring;

  public MultibindsInsteadOfMultibindingsTest(
      String moduleAnnotationType, String providesOrProduces, boolean importModuleAnnotation) {
    this.providesOrProduces = providesOrProduces;
    this.importModuleAnnotations = importModuleAnnotation;
    this.moduleAnnotation =
        importModuleAnnotations
            ? moduleAnnotationType.replaceAll(".*\\.", "")
            : moduleAnnotationType;
    this.moduleAnnotationImport =
        importModuleAnnotations ? "import " + moduleAnnotationType + ";" : "";
  }

  @Before
  public void setUp() {
    refactoring =
        BugCheckerRefactoringTestHelper.newInstance(
            new MultibindsInsteadOfMultibindings(), getClass());
  }

  @Test
  public void concreteModuleWithoutArguments() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = Test.Multi.class)",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteModuleWithSingleIncludesArgument() throws IOException {
    refactoring
        .addInputLines("in/OtherModule.java", "@dagger.Module final class OtherModule {}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = OtherModule.class)",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {OtherModule.class, Test.Multi.class})",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteModuleWithArrayIncludesArgument() throws IOException {
    refactoring
        .addInputLines("in/OtherModule.java", "@dagger.Module final class OtherModule {}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {OtherModule.class})",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {OtherModule.class, Test.Multi.class})",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteModuleWithArrayIncludesArgumentWithTrailingComma() throws IOException {
    refactoring
        .addInputLines("in/OtherModule.java", "@dagger.Module final class OtherModule {}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {OtherModule.class, })",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {OtherModule.class, Test.Multi.class, })",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteModuleWithEmptyIncludesArgument() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {})",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = Test.Multi.class)",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteModuleWithTwoMultibindings() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @Multibindings interface Multi1 {",
            "    Set<Object> set1();",
            "    Map<String, Object> map1();",
            "    @Qual Set<Object> qualifiedSet1();",
            "    @Qual Map<String, Object> qualifiedMap1();",
            "  }",
            "  @Multibindings interface Multi2 {",
            "    Set<Object> set2();",
            "    Map<String, Object> map2();",
            "    @Qual Set<Object> qualifiedSet2();",
            "    @Qual Map<String, Object> qualifiedMap2();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {Test.Multi1.class, Test.Multi2.class})",
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides Object instanceMethod() { return null; }",
            "  @" + moduleAnnotation + " interface Multi1 {",
            "    @Multibinds Set<Object> set1();",
            "    @Multibinds Map<String, Object> map1();",
            "    @Multibinds @Qual Set<Object> qualifiedSet1();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap1();",
            "  }",
            "  @" + moduleAnnotation + " interface Multi2 {",
            "    @Multibinds Set<Object> set2();",
            "    @Multibinds Map<String, Object> map2();",
            "    @Multibinds @Qual Set<Object> qualifiedSet2();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap2();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void interfaceModule() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "interface Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "interface Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibinds Set<Object> set();",
            "  @Multibinds Map<String, Object> map();",
            "  @Multibinds @Qual Set<Object> qualifiedSet();",
            "  @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "}")
        .doTest();
  }

  @Test
  public void abstractModule() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibinds abstract Set<Object> set();",
            "  @Multibinds abstract Map<String, Object> map();",
            "  @Multibinds abstract @Qual Set<Object> qualifiedSet();",
            "  @Multibinds abstract @Qual Map<String, Object> qualifiedMap();",
            "}")
        .doTest();
  }

  @Test
  public void canBecomeAbstractModule() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides static Object staticMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Provides static Object staticMethod() { return null; }",
            "  @Multibinds abstract Set<Object> set();",
            "  @Multibinds abstract Map<String, Object> map();",
            "  @Multibinds abstract @Qual Set<Object> qualifiedSet();",
            "  @Multibinds abstract @Qual Map<String, Object> qualifiedMap();",
            "}")
        .doTest();
  }

  @Test
  public void finalModuleCanBecomeAbstractModule() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "final class Test {",
            "  @Qualifier @interface Qual {}",
            "",
            "  @Provides",
            "  static Object staticMethod() {",
            "    return null;",
            "  }",
            "",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "",
            "  private Test() {} // No instances.",
            "",
            "  @Provides",
            "  static Object staticMethod() {",
            "    return null;",
            "  }",
            "",
            "  @Multibinds abstract Set<Object> set();",
            "  @Multibinds abstract Map<String, Object> map();",
            "  @Multibinds abstract @Qual Set<Object> qualifiedSet();",
            "  @Multibinds abstract @Qual Map<String, Object> qualifiedMap();",
            "}")
        .doTest();
  }

  @Test
  public void finalModuleWithConstructorCanBecomeAbstractModule() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import dagger.Provides;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "final class Test {",
            "  Test() { new Object().toString(); }",
            "  @Qualifier @interface Qual {}",
            "  @Provides static Object staticMethod() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.Provides;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  Test() { new Object().toString(); }",
            "  @Qualifier @interface Qual {}",
            "  @Provides static Object staticMethod() { return null; }",
            "  @Multibinds abstract Set<Object> set();",
            "  @Multibinds abstract Map<String, Object> map();",
            "  @Multibinds abstract @Qual Set<Object> qualifiedSet();",
            "  @Multibinds abstract @Qual Map<String, Object> qualifiedMap();",
            "}")
        .doTest();
  }

  @Test
  public void abstractModuleWithNameConflict() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @" + providesOrProduces + " static Object set() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = Test.Multi.class)",
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @" + providesOrProduces + " static Object set() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void abstractModuleWithInstanceProvidesMethods() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @" + providesOrProduces + " Object set() { return null; }",
            "  @Multibindings interface Multi {",
            "    Set<Object> set();",
            "    Map<String, Object> map();",
            "    @Qual Set<Object> qualifiedSet();",
            "    @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = Test.Multi.class)",
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @" + providesOrProduces + " Object set() { return null; }",
            "  @" + moduleAnnotation + " interface Multi {",
            "    @Multibinds Set<Object> set();",
            "    @Multibinds Map<String, Object> map();",
            "    @Multibinds @Qual Set<Object> qualifiedSet();",
            "    @Multibinds @Qual Map<String, Object> qualifiedMap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void abstractModuleWithTwoMultibindings() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibindings interface Multi1 {",
            "    Set<Object> set1();",
            "    Map<String, Object> map1();",
            "    @Qual Set<Object> qualifiedSet1();",
            "    @Qual Map<String, Object> qualifiedMap1();",
            "  }",
            "  @Multibindings interface Multi2 {",
            "    Set<Object> set2();",
            "    Map<String, Object> map2();",
            "    @Qual Set<Object> qualifiedSet2();",
            "    @Qual Map<String, Object> qualifiedMap2();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibinds abstract Set<Object> set1();",
            "  @Multibinds abstract Map<String, Object> map1();",
            "  @Multibinds @Qual abstract Set<Object> qualifiedSet1();",
            "  @Multibinds @Qual abstract Map<String, Object> qualifiedMap1();",
            "  @Multibinds abstract Set<Object> set2();",
            "  @Multibinds abstract Map<String, Object> map2();",
            "  @Multibinds @Qual abstract Set<Object> qualifiedSet2();",
            "  @Multibinds @Qual abstract Map<String, Object> qualifiedMap2();",
            "}")
        .doTest();
  }

  @Test
  public void abstractModuleWithTwoMultibindingsWithConflictingNames() throws IOException {
    refactoring
        .addInputLines(
            "in/Test.java",
            moduleAnnotationImport,
            "import dagger.Multibindings;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @Multibindings interface Multi1 {",
            "    Set<Object> set1();",
            "    Map<String, Object> map1();",
            "    @Qual Set<Object> qualifiedSet1();",
            "    @Qual Map<String, Object> sameName();",
            "  }",
            "  @Multibindings interface Multi2 {",
            "    Set<Object> set2();",
            "    Map<String, Object> map2();",
            "    @Qual Set<Object> qualifiedSet2();",
            "    @Qual Map<String, Object> sameName();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            moduleAnnotationImport,
            "import dagger.multibindings.Multibinds;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import javax.inject.Qualifier;",
            "",
            "@" + moduleAnnotation + "(includes = {Test.Multi1.class, Test.Multi2.class})",
            "abstract class Test {",
            "  @Qualifier @interface Qual {}",
            "  @" + moduleAnnotation + " interface Multi1 {",
            "    @Multibinds Set<Object> set1();",
            "    @Multibinds Map<String, Object> map1();",
            "    @Multibinds @Qual Set<Object> qualifiedSet1();",
            "    @Multibinds @Qual Map<String, Object> sameName();",
            "  }",
            "  @" + moduleAnnotation + " interface Multi2 {",
            "    @Multibinds Set<Object> set2();",
            "    @Multibinds Map<String, Object> map2();",
            "    @Multibinds @Qual Set<Object> qualifiedSet2();",
            "    @Multibinds @Qual Map<String, Object> sameName();",
            "  }",
            "}")
        .doTest();
  }
}
