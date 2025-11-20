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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE_ARRAY;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.ArrayList;
import java.util.List;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "Using a format string avoids string concatenation in the common case.",
    explanation =
        "It usually hurts performance to eagerly generate error messages with +, as you pay the"
            + " cost of the string conversion whether or not the condition fails. It's usually more"
            + " efficient to use %s as a placeholder and to pass the additional variables as"
            + " further arguments.",
    severity = WARNING)
public class FormatStringShouldUsePlaceholders extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final TreeVisitor<Void, List<ExpressionTree>> CONCATENATIONS =
      new SimpleTreeVisitor<Void, List<ExpressionTree>>() {
        @Override
        protected Void defaultAction(Tree tree, List<ExpressionTree> concats) {
          concats.add((ExpressionTree) tree);
          return null;
        }

        @Override
        public Void visitBinary(BinaryTree tree, List<ExpressionTree> concats) {
          if (tree.getKind() == Kind.PLUS) {
            tree.getLeftOperand().accept(this, concats);
            tree.getRightOperand().accept(this, concats);
            return null;
          } else {
            return super.visitBinary(tree, concats);
          }
        }
      };

  private static ImmutableList<ExpressionTree> formatArguments(
      MethodInvocationTree tree, VisitorState state) {
    ImmutableList<ExpressionTree> args = FormatStringUtils.formatMethodArguments(tree, state);
    if (!args.isEmpty()) {
      return args;
    }
    int index = LenientFormatStringUtils.getLenientFormatStringPosition(tree, state);
    if (index != -1) {
      return ImmutableList.copyOf(tree.getArguments().subList(index, tree.getArguments().size()));
    }
    return ImmutableList.of();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    final ImmutableList<? extends ExpressionTree> arguments = formatArguments(tree, state);
    if (arguments.isEmpty()) {
      return Description.NO_MATCH;
    }
    if (arguments.size() == 2
        && isSameType(getType(arguments.get(1)), OBJECT_TYPE_ARRAY.get(state), state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree formatString = arguments.getFirst();

    // If the value is a compile-time constant, either it has no concatenations or the
    // concatenations result in a constant expression that doesn't need inlining.
    if (ASTHelpers.constValue(formatString, String.class) != null) {
      return Description.NO_MATCH;
    }

    List<ExpressionTree> concats = new ArrayList<ExpressionTree>();
    formatString.accept(CONCATENATIONS, concats);
    if (concats.size() <= 1) {
      return Description.NO_MATCH;
    }

    StringBuilder newMessage = new StringBuilder("\"");
    StringBuilder newArgs = new StringBuilder();

    // Start this at the error string.
    // We will increment this as we pick up existing format string args and append them.
    int lastAddedArgPosition = 0;

    for (ExpressionTree concat : concats) {
      if (concat.getKind() == Kind.STRING_LITERAL) {
        String sourceString = state.getSourceForNode(concat);
        // 0th and last char is a double quote i.e. '"', so collect string without double quotes.
        newMessage.append(sourceString.subSequence(1, sourceString.length() - 1));

        // Figure out how far to advance the argument pointer.
        int len = sourceString.length();
        for (int i = 0; i < len; ++i) {
          char c = sourceString.charAt(i);
          if (c == '%' && i < len - 1) {
            if (sourceString.charAt(++i) == 's' && lastAddedArgPosition < arguments.size() - 1) {
              newArgs
                  .append(", ")
                  .append(state.getSourceForNode(arguments.get(++lastAddedArgPosition)));
            }
          }
        }
      } else {
        newMessage.append("%s");
        newArgs.append(", ").append(state.getSourceForNode(concat));
      }
    }
    // If we have any remaining args, add them as well.  Though this is a bug, so if this ever
    // gets triggered maybe we should just revert this until we can make a more proper fix.
    if (lastAddedArgPosition < arguments.size() - 1) {
      return Description.NO_MATCH;
    }
    if (newArgs.length() == 0) {
      return Description.NO_MATCH;
    }

    // Match the number of %s in format string and number of arguments.
    int numberPercentS = newMessage.toString().split("%s", -1).length - 1;
    int numberArgs = newArgs.toString().split(",", -1).length - 1;

    // we attempt to fix an extra `%s` at the end.
    if ((numberPercentS == numberArgs + 1) && newMessage.toString().endsWith("%s")) {
      // there is exactly one `%s` extra and that too at the end, lets remove it
      // For example, we'll have transformed the erroneous log("result: %s" + result) into
      // log("result: %s%s", result), which can be fixed by removing a trailing %s.
      newMessage.delete(newMessage.length() - 2, newMessage.length());
    } else if (numberArgs != numberPercentS) {
      return Description.NO_MATCH;
    }

    // Replace all the arguments from the "format string %s message " + stuff, all the way
    // to the end of the arg list.  Here's a drawing:
    // replace between:          v                            v
    // checkState(foo != false, "old message %s " + stuff, foo);
    // ->
    // checkState(foo != false, "new message %s %s", foo, stuff);
    // `arguments` already contains only the format string and the values to be substituted in.
    int start = getStartPosition(arguments.getFirst());
    int end = state.getEndPosition(arguments.getLast());

    String replacement = new StringBuilder(newMessage).append("\"").append(newArgs).toString();
    Fix fix = SuggestedFix.replace(start, end, replacement);
    return describeMatch(tree, fix);
  }
}
