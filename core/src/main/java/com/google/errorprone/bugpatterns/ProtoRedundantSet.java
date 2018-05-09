/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.TRUTH;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks that protocol buffers built with chained builders don't set the same field twice.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "ProtoRedundantSet",
    summary = "A field on a protocol buffer was set twice in the same chained expression.",
    category = TRUTH,
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class ProtoRedundantSet extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String DESCRIPTION =
      "%s. Setting the same field multiple times in a proto builder might be hiding a bug, "
          + "especially if it's set to different values.";

  /** Matches a chainable proto builder method. */
  private static final Matcher<ExpressionTree> PROTO_FLUENT_METHOD =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.protobuf.GeneratedMessage.Builder",
              "com.google.protobuf.GeneratedMessageLite.Builder")
          .withNameMatching(Pattern.compile("^(set|add|clear|put).+"));

  /**
   * Matches a terminal proto builder method. That is, a chainable builder method which is either
   * not followed by another method invocation, or by a method invocation which is not a {@link
   * #PROTO_FLUENT_METHOD}.
   */
  private static final Matcher<ExpressionTree> TERMINAL_PROTO_FLUENT_METHOD =
      allOf(
          PROTO_FLUENT_METHOD,
          (tree, state) -> {
            Tree nextLeaf = state.getPath().getParentPath().getLeaf();
            return !(nextLeaf instanceof ExpressionTree)
                || !PROTO_FLUENT_METHOD.matches((ExpressionTree) nextLeaf, state);
          });

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!TERMINAL_PROTO_FLUENT_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    ListMultimap<ProtoField, String> setters = ArrayListMultimap.create();
    SuggestedFix.Builder suggestedFixes = SuggestedFix.builder();
    Type type = ASTHelpers.getReturnType(tree);
    for (ExpressionTree current = tree;
        PROTO_FLUENT_METHOD.matches(current, state);
        current = ASTHelpers.getReceiver(current)) {
      MethodInvocationTree method = (MethodInvocationTree) current;
      if (!type.equals(ASTHelpers.getReturnType(current))) {
        break;
      }
      Symbol symbol = ASTHelpers.getSymbol(current);
      if (!(symbol instanceof MethodSymbol)) {
        break;
      }
      String methodName = symbol.getSimpleName().toString();
      // Break on methods like "addFooBuilder", otherwise we might be building a nested proto of the
      // same type.
      if (methodName.endsWith("Builder")) {
        break;
      }
      match(method, state, methodName, setters, suggestedFixes);
    }

    setters.asMap().entrySet().removeIf(entry -> entry.getValue().size() <= 1);

    if (setters.isEmpty()) {
      return Description.NO_MATCH;
    }

    String description =
        String.format(
            DESCRIPTION,
            setters
                .asMap()
                .entrySet()
                .stream()
                .map(entry -> describeField(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ")));
    return buildDescription(tree).setMessage(description).addFix(suggestedFixes.build()).build();
  }

  private static void match(
      MethodInvocationTree method,
      VisitorState state,
      String methodName,
      ListMultimap<ProtoField, String> setters,
      SuggestedFix.Builder suggestedFixes) {
    for (FieldType fieldType : FieldType.values()) {
      FieldWithValue match = fieldType.match(methodName, method);
      if (match != null) {
        Collection<String> previousArguments = setters.get(match.getField());
        // We flag up all duplicate sets, but only suggest a fix if the setter is given the same
        // argument (based on source code). This is to avoid the temptation to apply the fix in
        // cases like,
        //   MyProto.newBuilder().setFoo(copy.getFoo()).setFoo(copy.getBar())
        // where the correct fix is probably to replace the second 'setFoo' with 'setBar'.
        if (!previousArguments.isEmpty()
            && previousArguments.stream().allMatch(argument -> argument.equals(match.getValue()))) {
          int startPos = state.getEndPosition(ASTHelpers.getReceiver(method));
          int endPos = state.getEndPosition(method);
          suggestedFixes.replace(startPos, endPos, "");
        }
        setters.put(match.getField(), match.getValue());
      }
    }
  }

  private static String describeField(ProtoField field, Collection<String> arguments) {
    boolean sameArguments = arguments.stream().distinct().count() == 1;
    return String.format(
        "%s was called %s with %s",
        field,
        nTimes(arguments.size()),
        sameArguments ? "the same argument" : "different arguments");
  }

  private static final String nTimes(int n) {
    return n == 2 ? "twice" : String.format("%d times", n);
  }

  interface ProtoField {}

  enum FieldType {
    SINGLE {
      @Override
      FieldWithValue match(String name, MethodInvocationTree tree) {
        if (name.startsWith("set") && tree.getArguments().size() == 1) {
          return FieldWithValue.of(SingleField.of(name), tree.getArguments().get(0));
        }
        return null;
      }
    },
    REPEATED {
      @Override
      FieldWithValue match(String name, MethodInvocationTree tree) {
        if (name.startsWith("set") && tree.getArguments().size() == 2) {
          Integer index = ASTHelpers.constValue(tree.getArguments().get(0), Integer.class);
          if (index != null) {
            return FieldWithValue.of(RepeatedField.of(name, index), tree.getArguments().get(1));
          }
        }
        return null;
      }
    },
    MAP {
      @Override
      FieldWithValue match(String name, MethodInvocationTree tree) {
        if (name.startsWith("put") && tree.getArguments().size() == 2) {
          Object key = ASTHelpers.constValue(tree.getArguments().get(0), Object.class);
          if (key != null) {
            return FieldWithValue.of(MapField.of(name, key), tree.getArguments().get(1));
          }
        }
        return null;
      }
    };

    abstract FieldWithValue match(String name, MethodInvocationTree tree);
  }

  @AutoValue
  abstract static class SingleField implements ProtoField {
    abstract String getName();

    static SingleField of(String name) {
      return new AutoValue_ProtoRedundantSet_SingleField(name);
    }

    @Override
    public String toString() {
      return String.format("%s(..)", getName());
    }
  }

  @AutoValue
  abstract static class RepeatedField implements ProtoField {
    abstract String getName();

    abstract int getIndex();

    static RepeatedField of(String name, int index) {
      return new AutoValue_ProtoRedundantSet_RepeatedField(name, index);
    }

    @Override
    public String toString() {
      return String.format("%s(%s, ..)", getName(), getIndex());
    }
  }

  @AutoValue
  abstract static class MapField implements ProtoField {
    abstract String getName();

    abstract Object getKey();

    static MapField of(String name, Object key) {
      return new AutoValue_ProtoRedundantSet_MapField(name, key);
    }

    @Override
    public String toString() {
      return String.format("%s(%s, ..)", getName(), getKey());
    }
  }

  @AutoValue
  abstract static class FieldWithValue {
    abstract ProtoField getField();

    abstract String getValue();

    static FieldWithValue of(ProtoField field, ExpressionTree argumentTree) {
      return new AutoValue_ProtoRedundantSet_FieldWithValue(field, argumentTree.toString());
    }
  }
}
