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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.RemoveUnusedImports;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.doctree.LinkTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
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
import java.util.stream.Stream;
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
  @BugPattern(
      name = "EditModifiers",
      category = ONE_OFF,
      summary = "Edits modifiers",
      severity = ERROR,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
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
          Stream.of(editModifiers.value())
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
  @BugPattern(
      category = ONE_OFF,
      name = "CastReturn",
      severity = ERROR,
      summary = "Adds casts to returned expressions",
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
  @BugPattern(
      category = ONE_OFF,
      name = "CastReturn",
      severity = ERROR,
      summary = "Adds casts to returned expressions",
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
  @BugPattern(
      name = "AddAnnotation",
      category = Category.JDK,
      summary = "Add an annotation",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
      category = Category.JDK,
      summary = "Change the method return type",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
      category = Category.JDK,
      summary = "Change the method return type",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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

  /** A test check that qualifies javadoc link. */
  @BugPattern(
      name = "JavadocQualifier",
      category = Category.JDK,
      summary = "all javadoc links should be qualified",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
        public Void visitLink(LinkTree node, Void aVoid) {
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

  @BugPattern(
      name = "SuppressMe",
      category = ONE_OFF,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
  static final class SuppressMe extends BugChecker implements LiteralTreeMatcher {
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
  }

  @Test
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
            "}")
        .doTest();
  }

  @BugPattern(
      name = "SuppressMeWithComment",
      category = ONE_OFF,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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

  /** A test bugchecker that deletes any field whose removal doesn't break the compilation. */
  @BugPattern(
      name = "CompilesWithFixChecker",
      category = JDK,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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

  /** A test bugchecker that deletes an exception from throws. */
  @BugPattern(
      name = "RemovesExceptionChecker",
      category = JDK,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
  @BugPattern(
      name = "RenamesVariableChecker",
      category = JDK,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
  @BugPattern(
      name = "RemoveAddModifier",
      category = JDK,
      summary = "",
      severity = ERROR,
      providesFix = REQUIRES_HUMAN_ATTENTION)
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
      severity = ERROR,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
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
}
