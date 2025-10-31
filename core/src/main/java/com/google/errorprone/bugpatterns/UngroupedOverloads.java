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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.element.Name;

/**
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@BugPattern(
    summary =
        "Constructors and methods with the same name should appear sequentially with no other code"
            + " in between, even when modifiers such as static or private differ between the"
            + " methods. Please re-order or re-name methods.",
    severity = SUGGESTION)
public class UngroupedOverloads extends BugChecker implements ClassTreeMatcher {

  private final Boolean batchFindings;

  @Inject
  UngroupedOverloads(ErrorProneFlags flags) {
    batchFindings = flags.getBoolean("UngroupedOverloads:BatchFindings").orElse(false);
  }

  private record MemberWithIndex(int index, MethodTree tree) {
    static MemberWithIndex create(int index, MethodTree tree) {
      return new MemberWithIndex(index, tree);
    }
  }

  private record OverloadKey(Name name) {
    // TODO(cushon): re-enable `boolean isStatic`, but don't warn about interspersed
    // static/non-static overloads Include static-ness when grouping overloads. Static and
    // non-static methods are still overloads per the JLS, but are less interesting in practice.

    static OverloadKey create(MethodTree methodTree) {
      MethodSymbol sym = ASTHelpers.getSymbol(methodTree);
      return new OverloadKey(sym.getSimpleName());
    }
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    // collect all member methods and their indices in the list of members, grouped by name
    LinkedHashMultimap<OverloadKey, MemberWithIndex> methods = LinkedHashMultimap.create();
    for (int i = 0; i < classTree.getMembers().size(); ++i) {
      Tree member = classTree.getMembers().get(i);
      if (member instanceof MethodTree methodTree) {
        if (!ASTHelpers.isGeneratedConstructor(methodTree)) {
          methods.put(OverloadKey.create(methodTree), MemberWithIndex.create(i, methodTree));
        }
      }
    }
    ImmutableList<Description> descriptions =
        methods.asMap().entrySet().stream()
            .flatMap(
                e ->
                    checkOverloads(
                        state, classTree.getMembers(), ImmutableList.copyOf(e.getValue())))
            .collect(toImmutableList());
    if (batchFindings && !descriptions.isEmpty()) {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      descriptions.forEach(d -> fix.merge((SuggestedFix) getOnlyElement(d.fixes)));
      return describeMatch(descriptions.getFirst().position, fix.build());
    }
    descriptions.forEach(state::reportMatch);
    return NO_MATCH;
  }

  private Stream<Description> checkOverloads(
      VisitorState state, List<? extends Tree> members, ImmutableList<MemberWithIndex> overloads) {
    if (overloads.size() <= 1) {
      return Stream.empty();
    }
    // check if the indices of the overloads in the member list are sequential
    MemberWithIndex first = overloads.getFirst();
    int prev = -1;
    int group = 0;
    Map<MemberWithIndex, Integer> groups = new LinkedHashMap<>();
    for (MemberWithIndex overload : overloads) {
      if (prev != -1 && prev != overload.index() - 1) {
        group++;
      }
      groups.put(overload, group);
      prev = overload.index();
    }
    if (group == 0) {
      return Stream.empty();
    }
    if (overloads.stream().anyMatch(m -> isSuppressed(m.tree(), state))) {
      return Stream.empty();
    }
    // build a fix that replaces the first overload with all the overloads grouped together
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    StringBuilder sb = new StringBuilder("\n");
    sb.append(state.getSourceForNode(first.tree()));
    overloads.stream()
        .filter(o -> o != first)
        .forEach(
            o -> {
              int start = state.getEndPosition(members.get(o.index() - 1));
              int end = state.getEndPosition(o.tree());
              sb.append(state.getSourceCode(), start, end).append('\n');
              fixBuilder.replace(start, end, "");
            });
    fixBuilder.replace(first.tree(), sb.toString());
    SuggestedFix fix = fixBuilder.build();
    LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();
    // emit findings for each overload
    return overloads.stream()
        .map(
            o ->
                buildDescription(o.tree())
                    .addFix(fix)
                    .setMessage(createMessage(o.tree(), overloads, groups, lineMap, o))
                    .build());
  }

  private static String createMessage(
      MethodTree tree,
      ImmutableList<MemberWithIndex> overloads,
      Map<MemberWithIndex, Integer> groups,
      LineMap lineMap,
      MemberWithIndex current) {
    String ungroupedLines =
        overloads.stream()
            .filter(o -> !groups.get(o).equals(groups.get(current)))
            .map(t -> lineMap.getLineNumber(getStartPosition(t.tree())))
            .map(String::valueOf)
            .collect(joining(", "));
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    String name =
        symbol.isConstructor()
            ? "constructor overloads"
            : String.format("overloads of '%s'", symbol.getSimpleName());
    return String.format(
        "Overloads should be grouped together, even when modifiers such as static or private differ"
            + " between the methods; found ungrouped %s on line(s): %s",
        name, ungroupedLines);
  }
}
