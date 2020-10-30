/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.fixes;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.RemoveUnusedImports;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.RuntimeVersion;
import com.sun.source.doctree.LinkTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.JCTree;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import javax.lang.model.element.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class SuggestedFixesTest {

  /** Edit modifiers type. */
  @Retention(RUNTIME)
  public @interface EditModifiers {
    String[] value() default {};

    EditKind kind() default EditKind.ADD;

    /** Kind of edit. */
    enum EditKind {
      ADD,
      REMOVE
    }
  }

  /** Test checker that adds or removes modifiers. */
  @BugPattern(name = "EditModifiers", summary = "Edits modifiers", severity = ERROR)
  public static class EditModifiersChecker extends BugChecker
      implements VariableTreeMatcher, MethodTreeMatcher {

    static final ImmutableMap<String, Modifier> MODIFIERS_BY_NAME = createModifiersByName();

    private static ImmutableMap<String, Modifier> createModifiersByName() {
      ImmutableMap.Builder<String, Modifier> builder = ImmutableMap.builder();
      for (Modifier mod : Modifier.values()) {
        builder.put(mod.toString(), mod);
      }
      return builder.build();
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return editModifiers(tree, state);
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return editModifiers(tree, state);
    }

    private Description editModifiers(Tree tree, VisitorState state) {
      EditModifiers editModifiers =
          ASTHelpers.getAnnotation(
              ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class), EditModifiers.class);
      SuggestedFix.Builder fix = SuggestedFix.builder();
      Modifier[] mods =
          Arrays.stream(editModifiers.value())
              .map(v -> Verify.verifyNotNull(MODIFIERS_BY_NAME.get(v), v))
              .toArray(Modifier[]::new);
      switch (editModifiers.kind()) {
        case ADD:
          fix.merge(SuggestedFixes.addModifiers(tree, state, mods).orElse(null));
          break;
        case REMOVE:
          fix.merge(SuggestedFixes.removeModifiers(tree, state, mods).orElse(null));
          break;
        default:
          throw new AssertionError(editModifiers.kind());
      }
      return describeMatch(tree, fix.build());
    }
  }

  @Test
  public void addAtBeginningOfLine() {
    BugCheckerRefactoringTestHelper.newInstance(new EditModifiersChecker(), getClass())
        .addInputLines(
            "in/Test.java",
            "import javax.annotation.Nullable;",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "@EditModifiers(value=\"final\", kind=EditModifiers.EditKind.ADD)",
            "class Test {",
            "  @Nullable",
            "  int foo() {",
            "    return 10;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import javax.annotation.Nullable;",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "@EditModifiers(value=\"final\", kind=EditModifiers.EditKind.ADD)",
            "class Test {",
            "  @Nullable",
            "  final int foo() {",
            "    return 10;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void addModifiers() {
    CompilationTestHelper.newInstance(EditModifiersChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "import javax.annotation.Nullable;",
            "@EditModifiers(value=\"final\", kind=EditModifiers.EditKind.ADD)",
            "class Test {",
            "  // BUG: Diagnostic contains: final Object one",
            "  Object one;",
            "  // BUG: Diagnostic contains: @Nullable final Object two",
            "  @Nullable Object two;",
            "  // BUG: Diagnostic contains: @Nullable public final Object three",
            "  @Nullable public Object three;",
            "  // BUG: Diagnostic contains: public final Object four",
            "  public Object four;",
            "  // BUG: Diagnostic contains: public final @Nullable Object five",
            "  public @Nullable Object five;",
            "}")
        .doTest();
  }

  @Test
  public void addModifiersComment() {
    CompilationTestHelper.newInstance(EditModifiersChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "import javax.annotation.Nullable;",
            "@EditModifiers(value=\"final\", kind=EditModifiers.EditKind.ADD)",
            "class Test {",
            "  // BUG: Diagnostic contains:"
                + " private @Deprecated /*comment*/ final volatile Object one;",
            "  private @Deprecated /*comment*/ volatile Object one;",
            "  // BUG: Diagnostic contains:"
                + " private @Deprecated /*comment*/ static final Object two = null;",
            "  private @Deprecated /*comment*/ static Object two = null;",
            "}")
        .doTest();
  }

  @Test
  public void addModifiersFirst() {
    CompilationTestHelper.newInstance(EditModifiersChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "import javax.annotation.Nullable;",
            "@EditModifiers(value=\"public\", kind=EditModifiers.EditKind.ADD)",
            "class Test {",
            "  // BUG: Diagnostic contains: public static final transient Object one",
            "  static final transient Object one = null;",
            "}")
        .doTest();
  }

  @Test
  public void removeModifiers() {
    CompilationTestHelper.newInstance(EditModifiersChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "import javax.annotation.Nullable;",
            "@EditModifiers(value=\"final\", kind=EditModifiers.EditKind.REMOVE)",
            "class Test {",
            "  // BUG: Diagnostic contains: Object one",
            "  final Object one = null;",
            "  // BUG: Diagnostic contains: @Nullable Object two",
            "  @Nullable final Object two = null;",
            "  // BUG: Diagnostic contains: @Nullable public Object three",
            "  @Nullable public final Object three = null;",
            "  // BUG: Diagnostic contains: public Object four",
            "  public final Object four = null;",
            "}")
        .doTest();
  }

  @Test
  public void removeMultipleModifiers() {
    CompilationTestHelper.newInstance(EditModifiersChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", EditModifiers.class.getCanonicalName()),
            "import javax.annotation.Nullable;",
            "@EditModifiers(value={\"final\", \"static\"}, kind=EditModifiers.EditKind.REMOVE)",
            "class Test {",
            "  // BUG: Diagnostic contains: private Object one = null;",
            "  private static final Object one = null;",
            "}")
        .doTest();
  }

  /** Test checker that casts returned expression. */
  @BugPattern(name = "CastReturn", severity = ERROR, summary = "Adds casts to returned expressions")
  public static class CastReturn extends BugChecker implements ReturnTreeMatcher {

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (tree.getExpression() == null) {
        return Description.NO_MATCH;
      }
      Type type =
          ASTHelpers.getSymbol(ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class))
              .getReturnType();
      SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      String qualifiedTargetType = SuggestedFixes.qualifyType(state, fixBuilder, type.tsym);
      fixBuilder.prefixWith(tree.getExpression(), String.format("(%s) ", qualifiedTargetType));
      return describeMatch(tree, fixBuilder.build());
    }
  }

  /** Test checker that casts returned expression. */
  @BugPattern(name = "CastReturn", severity = ERROR, summary = "Adds casts to returned expressions")
  public static class CastReturnFullType extends BugChecker implements ReturnTreeMatcher {

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (tree.getExpression() == null) {
        return Description.NO_MATCH;
      }
      Type type =
          ASTHelpers.getSymbol(ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class))
              .getReturnType();
      SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      String qualifiedTargetType = SuggestedFixes.qualifyType(state, fixBuilder, type);
      fixBuilder.prefixWith(tree.getExpression(), String.format("(%s) ", qualifiedTargetType));
      return describeMatch(tree, fixBuilder.build());
    }
  }

  @Test
  public void qualifiedName_Object() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Object f() {",
            "    // BUG: Diagnostic contains: return (Object) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_imported() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.Map.Entry;",
            "class Test {",
            "  java.util.Map.Entry<String, Integer> f() {",
            "    // BUG: Diagnostic contains: return (Entry) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_notImported() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  java.util.Map.Entry<String, Integer> f() {",
            "    // BUG: Diagnostic contains: return (Map.Entry) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_typeVariable() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test<T> {",
            "  T f() {",
            "    // BUG: Diagnostic contains: return (T) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fullQualifiedName_Object() {
    CompilationTestHelper.newInstance(CastReturnFullType.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Object f() {",
            "    // BUG: Diagnostic contains: return (Object) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fullQualifiedName_imported() {
    CompilationTestHelper.newInstance(CastReturnFullType.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.Map.Entry;",
            "class Test {",
            "  java.util.Map.Entry<String, Integer> f() {",
            "    // BUG: Diagnostic contains: return (Entry<String,Integer>) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fullQualifiedName_notImported() {
    CompilationTestHelper.newInstance(CastReturnFullType.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  java.util.Map.Entry<String, Integer> f() {",
            "    // BUG: Diagnostic contains: return (Map.Entry<String,Integer>) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fullQualifiedName_typeVariable() {
    CompilationTestHelper.newInstance(CastReturnFullType.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test<T> {",
            "  T f() {",
            "    // BUG: Diagnostic contains: return (T) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  /** A test check that adds an annotation to all return types. */
  @BugPattern(name = "AddAnnotation", summary = "Add an annotation", severity = ERROR)
  public static class AddAnnotation extends BugChecker implements BugChecker.MethodTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      Type type = state.getTypeFromString("some.pkg.SomeAnnotation");
      SuggestedFix.Builder builder = SuggestedFix.builder();
      String qualifiedName = SuggestedFixes.qualifyType(state, builder, type);
      return describeMatch(
          tree.getReturnType(),
          builder.prefixWith(tree.getReturnType(), "@" + qualifiedName + " ").build());
    }

    private static BugCheckerRefactoringTestHelper testHelper(
        Class<? extends SuggestedFixesTest> clazz) {
      return BugCheckerRefactoringTestHelper.newInstance(new AddAnnotation(), clazz)
          .addInputLines(
              "in/some/pkg/SomeAnnotation.java",
              "package some.pkg;",
              "public @interface SomeAnnotation {}")
          .expectUnchanged();
    }
  }

  @Test
  public void qualifyType_alreadyImported() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/AddAnnotation.java",
            "import some.pkg.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation Void nullable = null;",
            "  Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "import some.pkg.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation Void nullable = null;",
            "  @SomeAnnotation Void foo() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_importType() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/AddAnnotation.java", "class AddAnnotation {", "  Void foo() { return null; }", "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "import some.pkg.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation Void foo() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_someOtherNullable() {
    AddAnnotation.testHelper(getClass())
        .addInputLines("in/SomeAnnotation.java", "@interface SomeAnnotation {}")
        .expectUnchanged()
        .addInputLines(
            "in/AddAnnotation.java",
            "class AddAnnotation {",
            "  @SomeAnnotation Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "class AddAnnotation {",
            "  @SomeAnnotation @some.pkg.SomeAnnotation Void foo() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_nestedNullable() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/AddAnnotation.java",
            "class AddAnnotation {",
            "  Void foo() { return null; }",
            "  @interface SomeAnnotation {}",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "class AddAnnotation {",
            "  @some.pkg.SomeAnnotation Void foo() { return null; }",
            "  @interface SomeAnnotation {}",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_deeplyNestedNullable() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/AddAnnotation.java",
            "class AddAnnotation {",
            "  Void foo() { return null; }",
            "  static class Nested {",
            "    Void bar() { return null; }",
            "    @interface SomeAnnotation {}",
            "  }",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "import some.pkg.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation Void foo() { return null; }",
            "  static class Nested {",
            "    @some.pkg.SomeAnnotation Void bar() { return null; }",
            "    @interface SomeAnnotation {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_someOtherNullableSomeOtherPackage() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/SomeAnnotation.java", "package foo.bar;", "public @interface SomeAnnotation {}")
        .expectUnchanged()
        .addInputLines(
            "in/AddAnnotation.java",
            "import foo.bar.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "import foo.bar.SomeAnnotation;",
            "class AddAnnotation {",
            "  @SomeAnnotation @some.pkg.SomeAnnotation Void foo() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyType_typeVariable() {
    AddAnnotation.testHelper(getClass())
        .addInputLines(
            "in/AddAnnotation.java",
            "class AddAnnotation {",
            "  <SomeAnnotation> Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/AddAnnotation.java",
            "class AddAnnotation {",
            "  <SomeAnnotation> @some.pkg.SomeAnnotation Void foo() { return null; }",
            "}")
        .doTest();
  }

  /** A test check that replaces all methods' return types with a given type. */
  @BugPattern(
      name = "ReplaceReturnType",
      summary = "Change the method return type",
      severity = ERROR)
  public static class ReplaceReturnType extends BugChecker implements BugChecker.MethodTreeMatcher {
    private final String newReturnType;

    public ReplaceReturnType(String newReturnType) {
      this.newReturnType = newReturnType;
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      Type type = state.getTypeFromString(newReturnType);
      SuggestedFix.Builder builder = SuggestedFix.builder();
      String qualifiedName = SuggestedFixes.qualifyType(state, builder, type);
      return describeMatch(
          tree.getReturnType(), builder.replace(tree.getReturnType(), qualifiedName).build());
    }
  }

  @Test
  public void qualifyType_nestedType() {
    qualifyNestedType(new ReplaceReturnType("pkg.Outer.Inner"));
  }

  @Test
  public void qualifyType_deeplyNestedType() {
    qualifyDeeplyNestedType(new ReplaceReturnType("pkg.Outer.Inner.Innermost"));
  }

  /** A test check that replaces all methods' return types with a given type. */
  @BugPattern(
      name = "ReplaceReturnTypeString",
      summary = "Change the method return type",
      severity = ERROR)
  public static class ReplaceReturnTypeString extends BugChecker
      implements BugChecker.MethodTreeMatcher {
    private final String newReturnType;

    public ReplaceReturnTypeString(String newReturnType) {
      this.newReturnType = newReturnType;
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      SuggestedFix.Builder builder = SuggestedFix.builder();
      String qualifiedName = SuggestedFixes.qualifyType(state, builder, newReturnType);
      return describeMatch(
          tree.getReturnType(), builder.replace(tree.getReturnType(), qualifiedName).build());
    }
  }

  @Test
  public void qualifyTypeString_nestedType() {
    qualifyNestedType(new ReplaceReturnTypeString("pkg.Outer.Inner"));
  }

  @Test
  public void qualifyTypeString_deeplyNestedType() {
    qualifyDeeplyNestedType(new ReplaceReturnTypeString("pkg.Outer.Inner.Innermost"));
  }

  @Test
  public void qualifiedName_canImportInnerClass() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceReturnTypeString("foo.A.B"), getClass())
        .addInputLines(
            "foo/A.java", //
            "package foo;",
            "public class A {",
            "  public static class B {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "bar/A.java", //
            "package bar;",
            "public class A extends foo.A {}")
        .expectUnchanged()
        .addInputLines(
            "bar/Test.java", //
            "package bar;",
            "public interface Test {",
            "  A.B foo();",
            "}")
        .addOutputLines(
            "bar/Test.java", //
            "package bar;",
            "import foo.A.B;",
            "public interface Test {",
            "  B foo();",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_outerAndInnerClassClash_fullyQualifies() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceReturnTypeString("foo.A.B"), getClass())
        .addInputLines(
            "foo/A.java", //
            "package foo;",
            "public class A {",
            "  public static class B {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "bar/A.java", //
            "package bar;",
            "public class A extends foo.A {}")
        .expectUnchanged()
        .addInputLines(
            "bar/B.java", //
            "package bar;",
            "public class B {}")
        .expectUnchanged()
        .addInputLines(
            "bar/Test.java", //
            "package bar;",
            "public interface Test {",
            "  A.B foo();",
            "}")
        .addOutputLines(
            "bar/Test.java", //
            "package bar;",
            "public interface Test {",
            "  foo.A.B foo();",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_noPackageName_noImportNeeded() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceReturnTypeString("A.B"), getClass())
        .addInputLines(
            "A.java", //
            "public interface A {",
            "  public static class B {}",
            "  B foo();",
            "  B bar();",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java", //
            "public interface Test {",
            "  A.B foo();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  private void qualifyDeeplyNestedType(BugChecker bugChecker) {
    BugCheckerRefactoringTestHelper.newInstance(bugChecker, getClass())
        .addInputLines(
            "in/pkg/Outer.java",
            "package pkg;",
            "public class Outer {",
            "  public class Inner {",
            "    public class Innermost {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/ReplaceReturnType.java",
            "class ReplaceReturnType {",
            "  Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/ReplaceReturnType.java",
            "import pkg.Outer;",
            "class ReplaceReturnType {",
            "  Outer.Inner.Innermost foo() { return null; }",
            "}")
        .doTest();
  }

  private void qualifyNestedType(BugChecker bugChecker) {
    BugCheckerRefactoringTestHelper.newInstance(bugChecker, getClass())
        .addInputLines(
            "in/pkg/Outer.java",
            "package pkg;",
            "public class Outer {",
            "  public class Inner {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/ReplaceReturnType.java",
            "class ReplaceReturnType {",
            "  Void foo() { return null; }",
            "}")
        .addOutputLines(
            "out/ReplaceReturnType.java",
            "import pkg.Outer;",
            "class ReplaceReturnType {",
            "  Outer.Inner foo() { return null; }",
            "}")
        .doTest();
  }

  /** A test check that replaces all checkNotNulls with verifyNotNull. */
  @BugPattern(
      name = "ReplaceMethodInvocations",
      summary = "Replaces checkNotNull with verifyNotNull.",
      severity = ERROR)
  public static class ReplaceMethodInvocations extends BugChecker
      implements BugChecker.MethodInvocationTreeMatcher {
    private static final Matcher<ExpressionTree> CHECK_NOT_NULL =
        staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!CHECK_NOT_NULL.matches(tree, state)) {
        return NO_MATCH;
      }
      SuggestedFix.Builder builder = SuggestedFix.builder();
      String qualifiedName =
          SuggestedFixes.qualifyStaticImport(
              "com.google.common.base.Verify.verifyNotNull", builder, state);
      return describeMatch(
          tree,
          builder
              .replace(
                  tree,
                  String.format(
                      "%s(%s)", qualifiedName, state.getSourceForNode(tree.getArguments().get(0))))
              .build());
    }
  }

  @Test
  public void qualifyStaticImport_addsStaticImportAndUsesUnqualifiedName() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceMethodInvocations(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  void test() {",
            "    checkNotNull(1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import static com.google.common.base.Verify.verifyNotNull;",
            "class Test {",
            "  void test() {",
            "    verifyNotNull(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyStaticImport_whenAlreadyImported_usesUnqualifiedName() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceMethodInvocations(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import static com.google.common.base.Verify.verifyNotNull;",
            "class Test {",
            "  void test() {",
            "    verifyNotNull(2);",
            "    checkNotNull(1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import static com.google.common.base.Verify.verifyNotNull;",
            "class Test {",
            "  void test() {",
            "    verifyNotNull(2);",
            "    verifyNotNull(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyStaticImport_whenNamesClash_usesQualifiedName() {
    BugCheckerRefactoringTestHelper.newInstance(new ReplaceMethodInvocations(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  void test() {",
            "    verifyNotNull(2);",
            "    checkNotNull(1);",
            "  }",
            "  void verifyNotNull(int a) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.common.base.Verify;",
            "class Test {",
            "  void test() {",
            "    verifyNotNull(2);",
            "    Verify.verifyNotNull(1);",
            "  }",
            "  void verifyNotNull(int a) {}",
            "}")
        .doTest();
  }

  /** A test check that qualifies javadoc link. */
  @BugPattern(
      name = "JavadocQualifier",
      summary = "all javadoc links should be qualified",
      severity = ERROR)
  public static class JavadocQualifier extends BugChecker implements BugChecker.ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, final VisitorState state) {
      final DCTree.DCDocComment comment =
          ((JCTree.JCCompilationUnit) state.getPath().getCompilationUnit())
              .docComments.getCommentTree((JCTree) tree);
      if (comment == null) {
        return Description.NO_MATCH;
      }
      final SuggestedFix.Builder fix = SuggestedFix.builder();
      new DocTreePathScanner<Void, Void>() {
        @Override
        public Void visitLink(LinkTree node, Void unused) {
          SuggestedFixes.qualifyDocReference(
              fix, new DocTreePath(getCurrentPath(), node.getReference()), state);
          return null;
        }
      }.scan(new DocTreePath(state.getPath(), comment), null);
      if (fix.isEmpty()) {
        return Description.NO_MATCH;
      }
      return describeMatch(tree, fix.build());
    }
  }

  @Test
  public void qualifyJavadocTest() {
    BugCheckerRefactoringTestHelper.newInstance(new JavadocQualifier(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import java.util.List;",
            "import java.util.Map;",
            "/** foo {@link List} bar {@link Map#containsKey(Object)} baz {@link #foo} */",
            "class Test {",
            "  void foo() {}",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import java.util.List;",
            "import java.util.Map;",
            "/** foo {@link java.util.List} bar {@link java.util.Map#containsKey(Object)} baz"
                + " {@link Test#foo} */",
            "class Test {",
            "  void foo() {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @BugPattern(name = "SuppressMe", summary = "", severity = ERROR)
  static final class SuppressMe extends BugChecker
      implements LiteralTreeMatcher, VariableTreeMatcher {
    @Override
    public Description matchLiteral(LiteralTree tree, VisitorState state) {
      if (tree.getValue().equals(42)) {
        Fix potentialFix = SuggestedFixes.addSuppressWarnings(state, "SuppressMe");
        if (potentialFix == null) {
          return describeMatch(tree);
        }
        return describeMatch(tree, potentialFix);
      }
      return Description.NO_MATCH;
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      // If it's a lambda param, then flag.
      LambdaExpressionTree enclosingMethod =
          ASTHelpers.findEnclosingNode(state.getPath(), LambdaExpressionTree.class);
      if (enclosingMethod != null && enclosingMethod.getParameters().contains(tree)) {
        return describeMatch(tree, SuggestedFixes.addSuppressWarnings(state, "AParameter"));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  @org.junit.Ignore("There appears to be an issue parsing lambda comments")
  public void testSuppressWarningsFix() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new SuppressMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  static final int BEST_NUMBER = 42;",
            "  static { int i = 42; }",
            "  @SuppressWarnings(\"one\")",
            "  public void doIt() {",
            "    System.out.println(\"\" + 42);",
            "    new Runnable() {",
            "      {System.out.println(\"\" + 42);}",
            "      @Override public void run() {}",
            "    };",
            "  }",
            "  public void bar() {",
            "    java.util.function.Predicate<String> p = x -> x.isEmpty();",
            "    java.util.function.Predicate<Integer> isEven = ",
            "        (Integer x) -> x % 2 == 0;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  @SuppressWarnings(\"SuppressMe\") static final int BEST_NUMBER = 42;",
            "  static { @SuppressWarnings(\"SuppressMe\") int i = 42; }",
            "  @SuppressWarnings({\"one\", \"SuppressMe\"})",
            "  public void doIt() {",
            "    System.out.println(\"\" + 42);",
            "    new Runnable() {",
            "      {System.out.println(\"\" + 42);}",
            "      @Override public void run() {}",
            "    };",
            "  }",
            "  public void bar() {",
            "    @SuppressWarnings(\"AParameter\")",
            "    java.util.function.Predicate<String> p = x -> x.isEmpty();",
            "    java.util.function.Predicate<Integer> isEven = ",
            "       (@SuppressWarnings(\"AParameter\") Integer x) -> x % 2 == 0;",
            "  }",
            "}")
        .doTest();
  }

  @BugPattern(name = "SuppressMeWithComment", summary = "", severity = ERROR)
  static final class SuppressMeWithComment extends BugChecker implements LiteralTreeMatcher {
    private final String lineComment;

    SuppressMeWithComment(String lineComment) {
      this.lineComment = lineComment;
    }

    @Override
    public Description matchLiteral(LiteralTree tree, VisitorState state) {
      Fix potentialFix = SuggestedFixes.addSuppressWarnings(state, "SuppressMe", lineComment);
      return describeMatch(tree, potentialFix);
    }
  }

  @Test
  public void testSuppressWarningsWithCommentFix() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new SuppressMeWithComment("b/XXXX: fix me!"), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  int BEST_NUMBER = 42;",
            "  @SuppressWarnings(\"x\") int BEST = 42;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  // b/XXXX: fix me!",
            "  @SuppressWarnings(\"SuppressMe\") int BEST_NUMBER = 42;",
            "  // b/XXXX: fix me!",
            "  @SuppressWarnings({\"x\", \"SuppressMe\"}) int BEST = 42;",
            "}")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void testSuppressWarningsWithCommentFix_existingComment() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new SuppressMeWithComment("b/XXXX: fix me!"), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  // This comment was here already.",
            "  int BEST_NUMBER = 42;",
            "",
            "  // As was this one.",
            "  @SuppressWarnings(\"x\") int BEST = 42;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  // This comment was here already.",
            "  // b/XXXX: fix me!",
            "  @SuppressWarnings(\"SuppressMe\") int BEST_NUMBER = 42;",
            "",
            "  // As was this one.",
            "  // b/XXXX: fix me!",
            "  @SuppressWarnings({\"x\", \"SuppressMe\"}) int BEST = 42;",
            "}")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void testSuppressWarningsWithCommentFix_commentHasToBeLineWrapped() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new SuppressMeWithComment(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
                    + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."),
            getClass());
    refactorTestHelper
        .addInputLines("in/Test.java", "public class Test {", "  int BEST = 42;", "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  // Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
                + "incididunt ut",
            "  // labore et dolore magna aliqua.",
            "  @SuppressWarnings(\"SuppressMe\") int BEST = 42;",
            "}")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @BugPattern(name = "RemoveSuppressFromMe", summary = "", severity = ERROR)
  static final class RemoveSuppressFromMe extends BugChecker implements LiteralTreeMatcher {

    @Override
    public Description matchLiteral(LiteralTree tree, VisitorState state) {
      SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      SuggestedFixes.removeSuppressWarnings(fixBuilder, state, "RemoveMe");
      return describeMatch(tree, fixBuilder.build());
    }
  }

  @Test
  public void removeSuppressWarnings_singleWarning_removesEntireAnnotation() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveSuppressFromMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  @SuppressWarnings(\"RemoveMe\") int BEST = 42;",
            "}")
        .addOutputLines("out/Test.java", "public class Test {", "  int BEST = 42;", "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void removeSuppressWarnings_twoWarning_removesWarningAndNewArray() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveSuppressFromMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  @SuppressWarnings({\"RemoveMe\", \"KeepMe\"}) int BEST = 42;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  @SuppressWarnings(\"KeepMe\") int BEST = 42;",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void removeSuppressWarnings_threeWarning_removesOnlyOneAndKeepsArray() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveSuppressFromMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  @SuppressWarnings({\"RemoveMe\", \"KeepMe1\", \"KeepMe2\"}) int BEST = 42;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  @SuppressWarnings({\"KeepMe1\", \"KeepMe2\"}) int BEST = 42;",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void removeSuppressWarnings_oneWarningInArray_removesWholeAnnotation() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveSuppressFromMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  @SuppressWarnings({\"RemoveMe\"}) int BEST = 42;",
            "}")
        .addOutputLines("out/Test.java", "public class Test {", "  int BEST = 42;", "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void removeSuppressWarnings_withValueInit_retainsValue() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveSuppressFromMe(), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  @SuppressWarnings(value={\"RemoveMe\", \"KeepMe\"}) int BEST = 42;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  @SuppressWarnings(value=\"KeepMe\") int BEST = 42;",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  /** A test bugchecker that deletes any field whose removal doesn't break the compilation. */
  @BugPattern(name = "CompilesWithFixChecker", summary = "", severity = ERROR)
  public static class CompilesWithFixChecker extends BugChecker implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Fix fix = SuggestedFix.delete(tree);
      return SuggestedFixes.compilesWithFix(fix, state)
          ? describeMatch(tree, fix)
          : Description.NO_MATCH;
    }
  }

  @Test
  public void compilesWithFixTest() {
    BugCheckerRefactoringTestHelper.newInstance(new CompilesWithFixChecker(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    int x = 0;",
            "    int y = 1;",
            "    System.err.println(y);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    int y = 1;",
            "    System.err.println(y);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void compilesWithFix_releaseFlag() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    BugCheckerRefactoringTestHelper.newInstance(new CompilesWithFixChecker(), getClass())
        .setArgs("--release", "9")
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    int x = 0;",
            "    int y = 1;",
            "    System.err.println(y);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f() {",
            "    int y = 1;",
            "    System.err.println(y);",
            "  }",
            "}")
        .doTest();
  }

  /** A test bugchecker that deletes an exception from throws. */
  @BugPattern(name = "RemovesExceptionChecker", summary = "", severity = ERROR)
  public static class RemovesExceptionsChecker extends BugChecker implements MethodTreeMatcher {

    private final int index;

    RemovesExceptionsChecker(int index) {
      this.index = index;
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      if (tree.getThrows().isEmpty() || tree.getThrows().size() <= index) {
        return NO_MATCH;
      }
      ExpressionTree expressionTreeToRemove = tree.getThrows().get(index);
      return describeMatch(
          expressionTreeToRemove,
          SuggestedFixes.deleteExceptions(tree, state, ImmutableList.of(expressionTreeToRemove)));
    }
  }

  @Test
  public void deleteExceptionsRemoveFirstCheckerTest() {
    BugCheckerRefactoringTestHelper.newInstance(new RemovesExceptionsChecker(0), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void e() {",
            "  }",
            "  void f() throws Exception {",
            "  }",
            "  void g() throws RuntimeException, Exception {",
            "  }",
            "  void h() throws RuntimeException, Exception, IOException {",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void e() {",
            "  }",
            "  void f() {",
            "  }",
            "  void g() throws Exception {",
            "  }",
            "  void h() throws Exception, IOException {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void deleteExceptionsRemoveSecondCheckerTest() {
    BugCheckerRefactoringTestHelper.newInstance(new RemovesExceptionsChecker(1), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void e() {",
            "  }",
            "  void f() throws Exception {",
            "  }",
            "  void g() throws RuntimeException, Exception {",
            "  }",
            "  void h() throws RuntimeException, Exception, IOException {",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "class Test {",
            "  void e() {",
            "  }",
            "  void f() throws Exception {",
            "  }",
            "  void g() throws RuntimeException {",
            "  }",
            "  void h() throws RuntimeException, IOException {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedImportInPackageInfo() {
    CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass())
        .addSourceLines(
            "in/com/example/package-info.java",
            "package com.example;",
            "// BUG: Diagnostic contains: Did you mean to remove this line?",
            "import java.util.Map;")
        .doTest();
  }

  /** Test checker that renames variables. */
  @BugPattern(name = "RenamesVariableChecker", summary = "", severity = ERROR)
  public static class RenamesVariableChecker extends BugChecker implements VariableTreeMatcher {

    private final String toReplace;
    private final String replacement;
    private final Class<?> typeClass;

    RenamesVariableChecker(String toReplace, String replacement, Class<?> typeClass) {
      this.toReplace = toReplace;
      this.replacement = replacement;
      this.typeClass = typeClass;
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      if (!tree.getName().contentEquals(toReplace) || !isSameType(typeClass).matches(tree, state)) {
        return Description.NO_MATCH;
      }
      return describeMatch(tree, SuggestedFixes.renameVariable(tree, replacement, state));
    }
  }

  @Test
  public void renameVariable_renamesLocalVariable_withNestedScope() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Integer.class), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void m() {",
            "    Integer replace = 0;",
            "    Integer b = replace;",
            "    if (true) {",
            "      Integer c = replace;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void m() {",
            "    Integer renamed = 0;",
            "    Integer b = renamed;",
            "    if (true) {",
            "      Integer c = renamed;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_ignoresMatchingNames_whenNotInScopeOfReplacement() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Integer.class), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void m() {",
            "    if (true) {",
            "      Integer replace = 0;",
            "      Integer c = replace;",
            "    }",
            "    Object replace = null;",
            "    Object c = replace;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void m() {",
            "    if (true) {",
            "      Integer renamed = 0;",
            "      Integer c = renamed;",
            "    }",
            "    Object replace = null;",
            "    Object c = replace;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_renamesMethodParameter() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Integer.class), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void m(Integer replace) {",
            "    Integer b = replace;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void m(Integer renamed) {",
            "    Integer b = renamed;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_renamesTryWithResourcesParameter() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", AutoCloseable.class), getClass())
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  abstract AutoCloseable open();",
            "  void m() {",
            "    try (AutoCloseable replace = open()) {",
            "      Object o = replace;",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  abstract AutoCloseable open();",
            "  void m() {",
            "    try (AutoCloseable renamed = open()) {",
            "      Object o = renamed;",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_renamesLambdaParameter_explicitlyTyped() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Integer.class), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Function<Integer, Integer> f = (Integer replace) -> replace;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Function<Integer, Integer> f = (Integer renamed) -> renamed;",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_renamesLambdaParameter_notExplicitlyTyped() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Integer.class), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Function<Integer, Integer> f = (replace) -> replace;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Function<Integer, Integer> f = (renamed) -> renamed;",
            "}")
        .doTest();
  }

  @Test
  public void renameVariable_renamesCatchParameter() {
    BugCheckerRefactoringTestHelper.newInstance(
            new RenamesVariableChecker("replace", "renamed", Throwable.class), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void m() {",
            "    try {",
            "    } catch (Throwable replace) {",
            "      replace.toString();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void m() {",
            "    try {",
            "    } catch (Throwable renamed) {",
            "      renamed.toString();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  /** Test checker that removes and adds modifiers in the same fix. */
  @BugPattern(name = "RemoveAddModifier", summary = "", severity = ERROR)
  public static class RemoveAddModifier extends BugChecker implements ClassTreeMatcher {

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .merge(SuggestedFixes.removeModifiers(tree, state, Modifier.PUBLIC).orElse(null))
              .merge(SuggestedFixes.addModifiers(tree, state, Modifier.ABSTRACT).orElse(null))
              .build());
    }
  }

  @Test
  public void removeAddModifier_rangesCompatible() {
    BugCheckerRefactoringTestHelper.newInstance(new RemoveAddModifier(), getClass())
        .addInputLines("in/Test.java", "public class Test {}")
        .addOutputLines("out/Test.java", "abstract class Test {}")
        .doTest();
  }

  /** A bugchecker for testing suggested fixes. */
  @BugPattern(
      name = "PrefixAddImportCheck",
      summary = "A bugchecker for testing suggested fixes.",
      severity = ERROR)
  public static class PrefixAddImportCheck extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .prefixWith(tree, "@Deprecated\n")
              .addImport("java.util.List")
              .build());
    }
  }

  @Test
  public void prefixAddImport() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new PrefixAddImportCheck(), getClass())
        .addInputLines(
            "in/Test.java", //
            "package p;",
            "class Test {}")
        .addOutputLines(
            "out/Test.java", //
            "package p;",
            "import java.util.List;",
            "@Deprecated",
            "class Test {}")
        .doTest();
  }

  @Test
  public void sourceURITest() throws Exception {
    assertThat(SuggestedFixes.sourceURI(URI.create("file:/com/google/Foo.java")))
        .isEqualTo(URI.create("file:/com/google/Foo.java"));
    assertThat(SuggestedFixes.sourceURI(URI.create("jar:file:sources.jar!/com/google/Foo.java")))
        .isEqualTo(URI.create("file:/com/google/Foo.java"));
  }

  @BugPattern(name = "RenameMethodChecker", summary = "RenameMethodChecker", severity = ERROR)
  private static class RenameMethodChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "singleton", state));
    }
  }

  @Test
  public void renameMethodInvocation() {
    BugCheckerRefactoringTestHelper.newInstance(new RenameMethodChecker(), getClass())
        .addInputLines(
            "Test.java",
            "import java.util.Collections;",
            "class Test {",
            "  int singletonList = 1;",
            "  Object foo = Collections.<Integer /* foo */>singletonList(singletonList);",
            "  Object bar = Collections.<Integer>/* foo */singletonList(singletonList);",
            "  Object baz = Collections.<Integer>  singletonList  (singletonList);",
            "  class emptyList {}",
            "  Object quux = Collections.<emptyList>singletonList(null);",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Collections;",
            "class Test {",
            "  int singletonList = 1;",
            "  Object foo = Collections.<Integer /* foo */>singleton(singletonList);",
            "  Object bar = Collections.<Integer>/* foo */singleton(singletonList);",
            "  Object baz = Collections.<Integer>  singleton  (singletonList);",
            "  class emptyList {}",
            "  Object quux = Collections.<emptyList>singleton(null);",
            "}")
        .doTest(TEXT_MATCH);
  }

  /**
   * Test checker that raises a diagnostic with the result of {@link SuggestedFixes#qualifyType} on
   * new instances.
   */
  @BugPattern(
      name = "QualifyTypeLocalClassChecker",
      summary = "QualifyTypeLocalClassChecker",
      severity = ERROR)
  public static class QualifyTypeLocalClassChecker extends BugChecker
      implements NewClassTreeMatcher {

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      SuggestedFix.Builder builder = SuggestedFix.builder();
      return buildDescription(tree)
          .setMessage(SuggestedFixes.qualifyType(state, builder, ASTHelpers.getType(tree).tsym))
          .build();
    }
  }

  @Test
  public void qualifyTypeLocal_localClass() {
    CompilationTestHelper.newInstance(QualifyTypeLocalClassChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.function.Supplier;",
            "class Test {",
            "  static {",
            "    class InStaticInitializer {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InStaticInitializer",
            "    new InStaticInitializer();",
            "  }",
            "",
            "  {",
            "    class InInstanceInitializer {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InInstanceInitializer",
            "    new InInstanceInitializer();",
            "  }",
            "",
            "  Test() { // in constructor",
            "    class InConstructor {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InConstructor",
            "    new InConstructor();",
            "  }",
            "",
            "  static Object staticMethod() {",
            "    class InStaticMethod {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InStaticMethod",
            "    return new InStaticMethod();",
            "  }",
            "",
            "  Object instanceMethod() {",
            "    class InInstanceMethod {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InInstanceMethod",
            "    return new InInstanceMethod();",
            "  }",
            "",
            "  void lambda() {",
            "    Supplier<Object> consumer = () -> {",
            "      class InLambda {}",
            "      // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] InLambda",
            "      return new InLambda();",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifyTypeLocal_anonymousClass() {
    CompilationTestHelper.newInstance(QualifyTypeLocalClassChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.function.Supplier;",
            "class Test {",
            "  // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "  static Object staticField = new Object() {};",
            "",
            "  // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "  Object instanceField = new Object() {};",
            "",
            "  static {",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "    new Object() {};",
            "  }",
            "",
            "  {",
            "    class InInstanceInitializer {}",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "    new Object() {};",
            "  }",
            "",
            "  Test() { // in constructor",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "    new Object() {};",
            "  }",
            "",
            "  static Object staticMethod() {",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "    return new Object() {};",
            "  }",
            "",
            "  Object instanceMethod() {",
            "    // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "    return new Object() {};",
            "  }",
            "",
            "  void lambda() {",
            "    Supplier<Object> consumer = () -> {",
            "      // BUG: Diagnostic contains: [QualifyTypeLocalClassChecker] Object",
            "      return new Object() {};",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  /** Test checker that adds @SuppressWarnings when compilation succeeds in the current unit. */
  @BugPattern(
      name = "AddSuppressWarningsIfCompilationSucceedsOnlyInSameCompilationUnit",
      summary = "",
      severity = ERROR)
  public static final class AddSuppressWarningsIfCompilationSucceedsOnlyInSameCompilationUnit
      extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return addSuppressWarningsIfCompilationSucceeds(
          tree, state, true, fix -> describeMatch(tree, fix));
    }
  }

  @Test
  public void compilesWithFix_onlyInSameCompilationUnit() {
    String[] unrelatedFile = {
      "class ClassContainingRawType {",
      // This unsuppressed raw type would prevent compilation.
      "  java.util.List list;",
      "}",
    };

    // This compilation will succeed because we only consider the compilation errors in the first
    // class.
    CompilationTestHelper.newInstance(
            AddSuppressWarningsIfCompilationSucceedsOnlyInSameCompilationUnit.class, getClass())
        .addSourceLines(
            "OnlyInSameCompilationUnit.java", //
            "// BUG: Diagnostic contains: foobar",
            "class OnlyInSameCompilationUnit {",
            "}")
        .addSourceLines("ClassContainingRawType.java", unrelatedFile)
        .doTest();

    // But a warning in the first class makes the compilation fail, so no suppression is added.
    CompilationTestHelper.newInstance(
            AddSuppressWarningsIfCompilationSucceedsOnlyInSameCompilationUnit.class, getClass())
        .addSourceLines(
            "OnlyInSameCompilationUnit.java", //
            "class OnlyInSameCompilationUnit {",
            // This unsuppressed raw type prevents compilation.
            "  java.util.List list;",
            "}")
        .addSourceLines("ClassContainingRawType.java", unrelatedFile)
        .doTest();
  }

  /** Test checker that adds @SuppressWarnings when compilation succeeds in all units. */
  @BugPattern(
      name = "AddSuppressWarningsIfCompilationSucceedsInAllCompilationUnits",
      summary = "",
      severity = ERROR)
  public static final class AddSuppressWarningsIfCompilationSucceedsInAllCompilationUnits
      extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return addSuppressWarningsIfCompilationSucceeds(
          tree, state, false, fix -> describeMatch(tree, fix));
    }
  }

  @Test
  public void compilesWithFix_inAllCompilationUnits() {
    // This compilation will succeed because we consider all compilation errors.
    CompilationTestHelper.newInstance(
            AddSuppressWarningsIfCompilationSucceedsInAllCompilationUnits.class, getClass())
        .addSourceLines(
            "InAllCompilationUnits.java", //
            "class InAllCompilationUnits {",
            "}")
        .addSourceLines(
            "ClassContainingRawType.java", //
            "class ClassContainingRawType {",
            // This unsuppressed raw type prevents re-compilation, so the other class is unchanged.
            "  java.util.List list;",
            "}")
        .doTest();
  }

  private static Description addSuppressWarningsIfCompilationSucceeds(
      ClassTree tree,
      VisitorState state,
      boolean onlyInSameCompilationUnit,
      Function<? super Fix, Description> toDescriptionFn) {
    return Optional.of(SuggestedFix.prefixWith(tree, "@SuppressWarnings(\"foobar\") "))
        .filter(
            fix ->
                SuggestedFixes.compilesWithFix(
                    fix,
                    state,
                    ImmutableList.of("-Xlint:unchecked,rawtypes", "-Werror"),
                    onlyInSameCompilationUnit))
        .map(toDescriptionFn)
        .orElse(NO_MATCH);
  }

  /** Test checker that casts return expressions to int. */
  @BugPattern(
      name = "AddSuppressWarningsIfCompilationSucceedsInAllCompilationUnits",
      summary = "",
      severity = ERROR)
  public static final class CastTreeToIntChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree.getExpression(), SuggestedFixes.castTree(tree.getExpression(), "int", state)));
    }
  }

  @Test
  public void castTree() {
    BugCheckerRefactoringTestHelper.newInstance(new CastTreeToIntChecker(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public int one() { return 1; }",
            "  public int negateOne() { return ~1; }",
            "  public int castOne() { return (short) 1; }",
            "  public int onePlusOne() { return 1 + 1; }",
            "  public int simpleAssignment() { int a = 0; return a = 1; }",
            "  public int compoundAssignment() { int a = 0; return a += 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  public int one() { return (int) 1; }",
            "  public int negateOne() { return (int) ~1; }",
            "  public int castOne() { return (int) (short) 1; }",
            "  public int onePlusOne() { return (int) (1 + 1); }",
            "  public int simpleAssignment() { int a = 0; return (int) (a = 1); }",
            "  public int compoundAssignment() { int a = 0; return (int) (a += 1); }",
            "}")
        .doTest();
  }

  @Test
  public void addDuplicateImport() {
    String firstImport = "java.time.Duration";
    String secondImport = "java.time.Instant";
    SuggestedFix fix =
        SuggestedFix.builder()
            .addImport(firstImport)
            .addImport(secondImport)
            .addImport(firstImport)
            .build();
    assertThat(fix.getImportsToAdd())
        .containsExactly("import " + firstImport, "import " + secondImport)
        .inOrder();
  }

  @Test
  public void addDuplicateStaticImport() {
    String firstImport = "java.util.concurrent.TimeUnit.MILLISECONDS";
    String secondImport = "java.util.concurrent.TimeUnit.SECONDS";
    SuggestedFix fix =
        SuggestedFix.builder()
            .addStaticImport(firstImport)
            .addStaticImport(secondImport)
            .addStaticImport(firstImport)
            .build();
    assertThat(fix.getImportsToAdd())
        .containsExactly("import static " + firstImport, "import static " + secondImport)
        .inOrder();
  }
}
