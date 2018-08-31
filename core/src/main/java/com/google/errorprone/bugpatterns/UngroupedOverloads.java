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

import static com.google.common.collect.Multimaps.toMultimap;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Streams;
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
import com.sun.tools.javac.tree.JCTree;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** @author hanuszczak@google.com (Łukasz Hanuszczak) */
@BugPattern(
    name = "UngroupedOverloads",
    summary =
        "Constructors and methods with the same name should appear sequentially"
            + " with no other code in between. Please re-order or re-name methods.",
    generateExamplesFromTestCases = false,
    category = JDK,
    severity = SUGGESTION,
    linkType = CUSTOM,
    tags = STYLE,
    link = "https://google.github.io/styleguide/javaguide.html#s3.4.2.1-overloads-never-split"
    )
public class UngroupedOverloads extends BugChecker implements ClassTreeMatcher {

  private final Boolean showFindingOnFirstOverloadOnly;

  public UngroupedOverloads(ErrorProneFlags flags) {
    showFindingOnFirstOverloadOnly =
        flags.getBoolean("UngroupedOverloads:FindingsOnFirstOverload").orElse(false);
  }

  @AutoValue
  abstract static class MemberWithIndex {

    abstract int index();

    abstract Tree tree();

    static MemberWithIndex create(int index, Tree tree) {
      return new AutoValue_UngroupedOverloads_MemberWithIndex(index, tree);
    }
  }

  @AutoValue
  abstract static class OverloadKey {
    abstract Name name();

    // TODO(cushon): re-enable this, but don't warn about interspersed static/non-static overloads
    // Include static-ness when grouping overloads. Static and non-static methods are still
    // overloads per the JLS, but are less interesting in practice.
    // abstract boolean isStatic();

    public static OverloadKey create(MethodTree methodTree) {
      MethodSymbol sym = ASTHelpers.getSymbol(methodTree);
      return new AutoValue_UngroupedOverloads_OverloadKey(sym.getSimpleName());
    }
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    // collect all member methods and their indices in the list of members, grouped by name
    LinkedHashMultimap<OverloadKey, MemberWithIndex> methods =
        Streams.zip(
                Stream.iterate(0, i -> i + 1),
                classTree.getMembers().stream(),
                MemberWithIndex::create)
            .filter(m -> m.tree() instanceof MethodTree)
            .collect(
                toMultimap(
                    m -> OverloadKey.create((MethodTree) m.tree()),
                    x -> x,
                    LinkedHashMultimap::create));
    methods
        .asMap()
        .forEach(
            (key, overloads) ->
                checkOverloads(
                    state, classTree.getMembers(), key.name(), ImmutableList.copyOf(overloads)));
    return NO_MATCH;
  }

  private void checkOverloads(
      VisitorState state,
      List<? extends Tree> members,
      Name name,
      ImmutableList<MemberWithIndex> overloads) {
    if (overloads.size() <= 1) {
      return;
    }
    // check if the indices of the overloads in the member list are sequential
    MemberWithIndex first = overloads.iterator().next();
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
      return;
    }
    if (overloads.stream().anyMatch(m -> isSuppressed(m.tree()))) {
      return;
    }
    // build a fix that deletes all but the first overload, and adds them back immediately after
    // the first overload
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    StringBuilder sb = new StringBuilder("\n");
    overloads.stream()
        .filter(o -> o != first)
        .forEach(
            o -> {
              int start = state.getEndPosition(members.get(o.index() - 1));
              int end = state.getEndPosition(o.tree());
              sb.append(state.getSourceCode(), start, end).append('\n');
              fixBuilder.replace(start, end, "");
            });
    fixBuilder.postfixWith(first.tree(), sb.toString());
    SuggestedFix fix = fixBuilder.build();
    LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();
    // emit findings for each overload
    overloads.stream()
        .limit(showFindingOnFirstOverloadOnly ? 1 : Long.MAX_VALUE)
        .forEach(
            o ->
                state.reportMatch(
                    buildDescription(o.tree())
                        .addFix(fix)
                        .setMessage(createMessage(name, overloads, groups, lineMap, o))
                        .build()));
  }

  private static String createMessage(
      Name name,
      ImmutableList<MemberWithIndex> overloads,
      Map<MemberWithIndex, Integer> groups,
      LineMap lineMap,
      MemberWithIndex current) {
    String ungroupedLines =
        overloads.stream()
            .filter(o -> !groups.get(o).equals(groups.get(current)))
            .map(t -> lineMap.getLineNumber(((JCTree) t.tree()).getStartPosition()))
            .map(String::valueOf)
            .collect(joining(", "));
    return String.format(
        "Overloads of '%s' are not grouped together; found ungrouped overloads on line(s): %s",
        name, ungroupedLines);
  }
}
