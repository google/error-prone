/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TryTree;

/** @author pvisontay@google.com */
@BugPattern(
    name = "BinderIdentityRestoredDangerously",
    tags = StandardTags.FRAGILE_CODE,
    summary =
        "A call to Binder.clearCallingIdentity() should be followed by "
            + "Binder.restoreCallingIdentity() in a finally block. Otherwise the wrong Binder "
            + "identity may be used by subsequent code.",
    severity = SeverityLevel.WARNING)
public class BinderIdentityRestoredDangerously extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String BINDER_CLASS_NAME = "android.os.Binder";
  private static final Matcher<ExpressionTree> RESTORE_IDENTITY_METHOD =
      MethodMatchers.staticMethod().onClass(BINDER_CLASS_NAME).named("restoreCallingIdentity");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    if (!RESTORE_IDENTITY_METHOD.matches(tree, state)) {
      return NO_MATCH;
    }

    // This is a simple implementation that doesn't have 100% accuracy - e.g. it would accept it
    // if both Binder.clearCallingIdentity() and Binder.restoreCallingIdentity() were in the same
    // finally {} block. But in practice it should work well for the large majority of existing
    // code.
    // TODO: Also detect when a clearCallingIdentity() call is not followed by
    // restoreCallingIdentity().
    TryTree enclosingTry = findEnclosingNode(state.getPath(), TryTree.class);
    if (enclosingTry == null) {
      return describeMatch(tree);
    }

    return NO_MATCH;
  }
}
