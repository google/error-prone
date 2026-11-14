/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isLikelyToBePublicApi;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModuleTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCModuleDecl;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link NullnessUtils}. */
@RunWith(TestParameterInjector.class)
public final class NullnessUtilsTest {
  public enum OverloadOfIsLikelyToBePublicApi {
    SYMBOL(SymbolLikelyPublicApiChecker.class),
    TREE_PATH(TreePathLikelyPublicApiChecker.class);

    final Class<? extends BugChecker> checker;

    OverloadOfIsLikelyToBePublicApi(Class<? extends BugChecker> checker) {
      this.checker = checker;
    }
  }

  @TestParameter private OverloadOfIsLikelyToBePublicApi overload;

  @Test
  public void isLikelyToBePublicApi_records() {
    compilationHelper()
        .addSourceLines(
            "PublicFoo.java",
            """
            // BUG: Diagnostic contains:
            public class PublicFoo {
              // BUG: Diagnostic contains:
              public record PublicRecord(
                  // BUG: Diagnostic contains:
                  Object component) {}

              // BUG: Diagnostic contains:
              protected record ProtectedRecord(
                  // BUG: Diagnostic contains:
                  Object component) {}

              record PackagePrivateRecord(Object component) {}

              private record PrivateRecord(Object component) {}
            }
            """)
        .addSourceLines(
            "PackagePrivateFoo.java",
            """
            class PackagePrivateFoo {
              public record PublicRecordInPackagePrivateClass(Object component) {}
            }
            """)
        .doTest();
  }

  @Test
  public void isLikelyToBePublicApi_methodsAndFields() {
    compilationHelper()
        .addSourceLines(
            "PublicFoo.java",
            """
            // BUG: Diagnostic contains:
            public class PublicFoo {
              // BUG: Diagnostic contains:
              public Object publicFieldWithoutInitializer;
              // BUG: Diagnostic contains:
              public Object publicFieldWithInitializer = null;
              Object packagePrivateField;

              // BUG: Diagnostic contains:
              public void publicMethod() {}

              void packagePrivateMethod() {}
            }
            """)
        .doTest();
  }

  @Test
  public void isLikelyToBePublicApi_package() {
    compilationHelper()
        .addSourceLines(
            "package-info.java",
            """
            // BUG: Diagnostic contains:
            package com.example;
            """)
        .doTest();
  }

  @Test
  public void isLikelyToBePublicApi_module() {
    compilationHelper()
        .addSourceLines(
            "module-info.java",
            """
            // BUG: Diagnostic contains:
            module com.example {}
            """)
        .doTest();
  }

  private CompilationTestHelper compilationHelper() {
    return CompilationTestHelper.newInstance(overload.checker, getClass());
  }

  /** Test checker for {@link NullnessUtils#isLikelyToBePublicApi(Symbol)}. */
  @BugPattern(summary = "Element is likely to be public API", severity = ERROR)
  public static final class SymbolLikelyPublicApiChecker extends BugChecker
      implements PackageTreeMatcher,
          ModuleTreeMatcher,
          ClassTreeMatcher,
          MethodTreeMatcher,
          VariableTreeMatcher {
    @Override
    public Description matchPackage(PackageTree tree, VisitorState state) {
      return check(tree, getSymbol(tree));
    }

    @Override
    public Description matchModule(ModuleTree tree, VisitorState state) {
      return check(tree, ((JCModuleDecl) tree).sym);
    }

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return check(tree, getSymbol(tree));
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return hasExplicitSource(tree, state) ? check(tree, getSymbol(tree)) : NO_MATCH;
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return check(tree, getSymbol(tree));
    }

    private Description check(Tree tree, Symbol sym) {
      return isLikelyToBePublicApi(sym) ? describeMatch(tree) : NO_MATCH;
    }
  }

  /** Test checker for {@link NullnessUtils#isLikelyToBePublicApi(TreePath)}. */
  @BugPattern(summary = "TreePath is likely to be public API", severity = ERROR)
  public static final class TreePathLikelyPublicApiChecker extends BugChecker
      implements PackageTreeMatcher,
          ModuleTreeMatcher,
          ClassTreeMatcher,
          MethodTreeMatcher,
          VariableTreeMatcher {
    @Override
    public Description matchPackage(PackageTree tree, VisitorState state) {
      return check(tree, state);
    }

    @Override
    public Description matchModule(ModuleTree tree, VisitorState state) {
      return check(tree, state);
    }

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return check(tree, state);
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return hasExplicitSource(tree, state) ? check(tree, state) : NO_MATCH;
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return check(tree, state);
    }

    private Description check(Tree tree, VisitorState state) {
      return isLikelyToBePublicApi(state.getPath()) ? describeMatch(tree) : NO_MATCH;
    }
  }
}
