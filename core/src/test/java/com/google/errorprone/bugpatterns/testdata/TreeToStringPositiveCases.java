/*
 * Copyright 2021 The Error Prone Authors.
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

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

public class TreeToStringPositiveCases {

  public static class InnerClass extends BugChecker {
    private static void foo() {
      Tree tree = (Tree) new Object();
      // BUG: Diagnostic contains: [TreeToString] Tree#toString shouldn't be used
      tree.toString();
    }

    private static final Matcher<ClassTree> MATCHER1 =
        (tree, state) -> {
          ExpressionTree packageName = state.getPath().getCompilationUnit().getPackageName();
          // BUG: Diagnostic contains: [TreeToString] Tree#toString shouldn't be used
          packageName.toString();

          // BUG: Diagnostic contains: [TreeToString] Tree#toString shouldn't be used
          state.getPath().getCompilationUnit().getPackageName().toString();

          return false;
        };

    private static final Matcher<ClassTree> MATCHER2 =
        new Matcher<ClassTree>() {
          @Override
          public boolean matches(ClassTree classTree, VisitorState state) {
            ExpressionTree packageName = state.getPath().getCompilationUnit().getPackageName();
            // TODO(b/206850765): This case is not caught by TreeToStringTest, but it *is* caught
            // when this Matcher is converted to a lambda as above. TreeToString should be
            // consistent for an anonymous class and the equivalent functional interface lambda
            // form.
            packageName.toString();
            return false;
          }
        };
  }
}
