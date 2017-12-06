package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.MethodInvocationTree;

@AutoService(BugChecker.class)
@BugPattern(name = "LinkedListChecker", summary = "java.util.LinkedList accessed through via index",
    severity = ERROR, category = Category.JDK)
public final class LinkedListChecker extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String LINKED_LIST_FQN = "java.util.LinkedList";
  private static final Matcher<MethodInvocationTree> INSTANCE_LINKEDLIST_MATCHER = Matchers.anyOf(
      MethodMatchers.instanceMethod().onDescendantOf(LINKED_LIST_FQN).named("add")
          .withParameters("int", "java.lang.Object"),
      MethodMatchers.instanceMethod().onDescendantOf(LINKED_LIST_FQN).named("addAll"),
      MethodMatchers.instanceMethod().onDescendantOf(LINKED_LIST_FQN).named("get"),
      MethodMatchers.instanceMethod().onDescendantOf(LINKED_LIST_FQN).named("remove")
          .withParameters("int"),
      MethodMatchers.instanceMethod().onDescendantOf(LINKED_LIST_FQN).named("set")
          .withParameters("int", "java.lang.Object"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!INSTANCE_LINKEDLIST_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    return this.describeMatch(tree);
  }

}
