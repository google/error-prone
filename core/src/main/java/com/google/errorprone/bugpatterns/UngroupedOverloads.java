/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;

/** @author hanuszczak@google.com (Łukasz Hanuszczak) */
@BugPattern(
  name = "UngroupedOverloads",
  summary =
      "Constructors and methods with the same name should appear sequentially"
          + " with no other code in between",
  generateExamplesFromTestCases = false,
  category = JDK,
  severity = SUGGESTION,
  linkType = CUSTOM,
  link = "https://google.github.io/styleguide/javaguide.html#s3.4.2.1-overloads-never-split"
)
public class UngroupedOverloads extends BugChecker implements ClassTreeMatcher {

  private static final int DEFAULT_METHOD_COUNT_CUTOFF = 100;

  private final int methodCountCutoff;

  public UngroupedOverloads() {
    this(DEFAULT_METHOD_COUNT_CUTOFF);
  }

  /**
   * @param methodCountCutoff the limit on the number of methods in the class for which fixes are no
   *     longer suggested (but correctness is still ensured).
   */
  UngroupedOverloads(int methodCountCutoff) {
    this.methodCountCutoff = methodCountCutoff;
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    List<? extends Tree> classMembers = new ArrayList<>(classTree.getMembers());
    MethodFixSuggester suggester = new MethodFixSuggester(classTree, classMembers, state);

    /*
     * Checking the class members for violations requires only in O(n) time whereas providing
     * suggested fixes requires O(n^2) time. This is why we have a cut-off limit that depending on
     * the number of class members selects feasible strategy.
     */
    long methodCount = classMembers.stream().filter((tree) -> tree instanceof MethodTree).count();
    if (methodCount >= methodCountCutoff) {
      checkMembers(classMembers, suggester);
    } else {
      orderMembers(classMembers, suggester);
    }
    return suggester.describeFix();
  }

  private static void checkMembers(
      List<? extends Tree> classMembers, MethodFixSuggester suggester) {
    Map<Name, Integer> previousOccurrences = new LinkedHashMap<>();
    for (int currentOccurrence = 0; currentOccurrence < classMembers.size(); currentOccurrence++) {
      Tree memberTree = classMembers.get(currentOccurrence);
      if (!(memberTree instanceof MethodTree)) {
        continue;
      }

      MethodTree methodTree = (MethodTree) memberTree;
      Name methodName = methodTree.getName();

      /*
       * If there is a previous occurrence and it is on a position different than the previous one
       * it must be the case that the methods are not ordered properly (so we report a violation).
       */
      Integer previousOccurrence = previousOccurrences.get(methodName);
      if (previousOccurrence != null && previousOccurrence != currentOccurrence - 1) {
        suggester.justReport(currentOccurrence);
      }
      previousOccurrences.put(methodName, currentOccurrence);
    }
  }

  private static void orderMembers(
      List<? extends Tree> classMembers, MethodFixSuggester suggester) {
    /*
     * The ordering algorithm works by bubbling (the same way as in bubble-sort) methods until they
     * reach correct position. Therefore algorithm has a O(n^2) where n is number of methods within
     * the class. However, this is just a worst case scenario (and classes usually don't have more
     * than 1000 members anyway) that could happen only for really, really weird code.
     *
     * The problem itself looks like something that could be solved in O(n lg n) time but it would
     * probably be much more complicated and the constant would be much greater (so for sane code
     * it would be slower).
     */
    Map<Name, Integer> previousOccurrences = new LinkedHashMap<>();
    for (int currentOccurrence = 0; currentOccurrence < classMembers.size(); currentOccurrence++) {
      Tree memberTree = classMembers.get(currentOccurrence);
      if (!(memberTree instanceof MethodTree)) {
        continue;
      }

      MethodTree methodTree = (MethodTree) memberTree;
      Name methodName = methodTree.getName();

      Integer previousOccurrence = previousOccurrences.get(methodName);
      if (previousOccurrence != null) {
        // If the block is actually moved (i.e. the `for` loop below does at least one iteration).
        if (currentOccurrence - 1 > previousOccurrence) {
          // We "bubble" the current occurrence until it is placed next to the previous occurrence.
          for (int i = currentOccurrence - 1; i > previousOccurrence; i--) {
            Tree splitterTree = classMembers.get(i);
            Name splitterName = getMemberName(splitterTree);

            // Swapping may invalidate `previousOccurrences` so we need to shift it by one manually.
            Integer splitterOccurrence = previousOccurrences.get(splitterName);
            if (splitterOccurrence != null && splitterOccurrence.equals(i)) {
              previousOccurrences.put(splitterName, i + 1);
            }

            Collections.swap(classMembers, i, i + 1);
          }
          suggester.moveBlock(currentOccurrence);
        }
        previousOccurrences.put(methodName, previousOccurrence + 1);
      } else {
        previousOccurrences.put(methodName, currentOccurrence);
      }
    }
  }

  /**
   * Returns a name for given {@code memberTree} declaration.
   *
   * <p>Unfortunately there is no specific {@code MemberTree} class and {@link
   * ClassTree#getMembers()} returns a list of {@link Tree} elements. But we know that the only
   * valid member declarations are either inner classes, methods or variables and they are all
   * named.
   */
  private static Name getMemberName(Tree memberTree) {
    if (memberTree instanceof ClassTree) {
      return ((ClassTree) memberTree).getSimpleName();
    }
    if (memberTree instanceof MethodTree) {
      return ((MethodTree) memberTree).getName();
    }
    if (memberTree instanceof VariableTree) {
      return ((VariableTree) memberTree).getName();
    }
    throw new AssertionError("expected member tree instead of " + memberTree.getKind());
  }

  /**
   * Returns a more fine-tuned starting position of a {@code current} node in the source code.
   *
   * <p>Unfortunately, {@link JCTree#getStartPosition()} doesn't account for comments which usually
   * are integral part of the code. So, instead we use a heuristic: the AST node actually "starts"
   * after the first newline after the end position of the previous AST node.
   *
   * <p>This assumes the relevant comments are placed either before (i.e. above) the definition or
   * after the definition on the same line rather than below the definition.
   *
   * @param current the node for which we retrieve the position
   * @param previous a node before the {@code current} one
   * @return more useful starting position of the {@code current} node
   */
  private static int getBroadStartPosition(VisitorState state, Tree current, Tree previous) {
    int previousEndPosition = getBroadEndPosition(state, previous, current);
    int currentStartPosition = ((JCTree) current).getStartPosition();
    return Math.min(previousEndPosition, currentStartPosition);
  }

  /**
   * Returns a more fine-tuned ending position of a {@code current} node in the source code.
   *
   * <p>See {@link #getBroadStartPosition(VisitorState, Tree, Tree)} for more information.
   *
   * @param current the node for which we retrieve the position
   * @param next a node after the {@code current} one (optional)
   * @return more useful ending position of the {@code current} node
   */
  private static int getBroadEndPosition(VisitorState state, Tree current, @Nullable Tree next) {
    CharSequence source = state.getSourceCode();

    int currentEndPosition = state.getEndPosition(current);
    int nextStartPosition;
    if (next != null) {
      nextStartPosition = ((JCTree) next).getStartPosition();
    } else {
      nextStartPosition = source.length();
    }

    String newline = System.lineSeparator();
    int newlinePosition = indexOf(source, newline, currentEndPosition, nextStartPosition);
    return (newlinePosition < 0) ? currentEndPosition : newlinePosition;
  }

  /**
   * Looks for the first occurrence of {@code term} in {@code sequence}.
   *
   * <p>This is analogous to the {@link java.lang.String#indexOf(String, int)} but works with any
   * {@link java.lang.CharSequence} and also supports {@code toIndex} parameter.
   *
   * <p>The algorithm has a O(nm) time complexity but it is more than enough for this use case.
   *
   * @param sequence the character sequence to perform the search on
   * @param term the character sequence to search for
   * @param fromIndex the index from which to start the search (inclusive)
   * @param toIndex the index to which the search happens (exclusive)
   * @return the index of the first occurrence of specified substring or -1 if not found
   */
  private static int indexOf(CharSequence sequence, CharSequence term, int fromIndex, int toIndex) {
    int termLength = term.length();
    for (int index = fromIndex; index + termLength - 1 < toIndex; index++) {
      int i = 0;
      for (; i < termLength; i++) {
        if (sequence.charAt(index + i) != term.charAt(i)) {
          break;
        }
      }
      if (i == termLength) {
        return index;
      }
    }
    return -1;
  }

  private interface OverloadViolation {
    Name getMethodName();

    void buildFix(SuggestedFix.Builder fix, VisitorState state, MethodTree target);
  }

  private static class JustReport implements OverloadViolation {
    private final MethodTree methodTree;

    public JustReport(MethodTree methodTree) {
      this.methodTree = methodTree;
    }

    @Override
    public Name getMethodName() {
      return methodTree.getName();
    }

    @Override
    public void buildFix(SuggestedFix.Builder fix, VisitorState state, MethodTree target) {
      // Do nothing, this is just for reporting violations.
    }
  }

  private static class MoveBlock implements OverloadViolation {
    private final Name methodName;
    private final int startPosition;
    private final int endPosition;

    public MoveBlock(Name methodName, int startPosition, int endPosition) {
      this.methodName = methodName;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
    }

    @Override
    public Name getMethodName() {
      return methodName;
    }

    @Override
    public void buildFix(SuggestedFix.Builder fix, VisitorState state, MethodTree target) {
      String methodSource = getMethodSource(state.getSourceCode());
      fix.replace(startPosition, endPosition, "");
      fix.postfixWith(target, methodSource);
    }

    public String getMethodSource(CharSequence sourceCode) {
      return sourceCode.subSequence(startPosition, endPosition).toString();
    }
  }

  private class MethodFixSuggester {

    private final ClassTree classTree;
    private final ImmutableList<? extends Tree> classMembers; // Initial, unchanged members.
    private final VisitorState state;
    private final List<OverloadViolation> violations;

    public MethodFixSuggester(
        ClassTree classTree, List<? extends Tree> classMembers, VisitorState state) {
      this.classTree = classTree;
      this.classMembers = ImmutableList.copyOf(classMembers);
      this.state = state;
      this.violations = new ArrayList<>();
    }

    public void justReport(int currentOccurrence) {
      violations.add(new JustReport((MethodTree) classMembers.get(currentOccurrence)));
    }

    public void moveBlock(int currentOccurrence) {
      MethodTree currentTree = (MethodTree) classMembers.get(currentOccurrence);
      Name currentName = currentTree.getName();

      Tree previousTree = classMembers.get(currentOccurrence - 1);
      Tree nextTree;
      if (currentOccurrence + 1 < classMembers.size()) {
        nextTree = classMembers.get(currentOccurrence + 1);
      } else {
        nextTree = null;
      }

      int startPosition = getBroadStartPosition(state, currentTree, previousTree);
      int endPosition = getBroadEndPosition(state, currentTree, nextTree);
      violations.add(new MoveBlock(currentName, startPosition, endPosition));
    }

    public Description describeFix() {
      if (violations.isEmpty()) {
        return Description.NO_MATCH;
      } else {
        return buildAdaptedDescription().addFix(buildFix()).build();
      }
    }

    private SuggestedFix buildFix() {
      ImmutableMap<Name, MethodTree> lastGroupedOccurrences = getLastGroupedOccurrences();

      SuggestedFix.Builder fix = SuggestedFix.builder();
      for (OverloadViolation violation : violations) {
        Name methodName = violation.getMethodName();
        violation.buildFix(fix, state, lastGroupedOccurrences.get(methodName));
      }
      return fix.build();
    }

    private Description.Builder buildAdaptedDescription() {
      ImmutableSet<Name> methodNames =
          violations.stream().map(OverloadViolation::getMethodName).collect(toImmutableSet());

      if (methodNames.size() == 1) {
        return buildMethodDescription(methodNames.iterator().next());
      } else {
        return buildClassDescription(methodNames);
      }
    }

    private Description.Builder buildMethodDescription(Name methodName) {
      MethodTree methodTree = getFirstOccurrences().get(methodName);
      return buildDescription(methodTree).setMessage(getMethodFixMessage());
    }

    private Description.Builder buildClassDescription(Collection<Name> methodNames) {
      return buildDescription(classTree).setMessage(getClassFixMessage(methodNames));
    }

    /**
     * Returns a mapping from a method name to the first (topmost) AST node matching this name.
     *
     * <p>For a class with methods {@code A}, {@code B} and {@code C} in the following order the
     * marked nodes are considered to be "first occurrences":
     *
     * <pre>
     * AABBBABCCAB
     * ^ ^    ^
     * </pre>
     */
    private ImmutableMap<Name, MethodTree> getFirstOccurrences() {
      Map<Name, MethodTree> firstOccurrences = new LinkedHashMap<>();
      for (Tree memberTree : classMembers) {
        if (!(memberTree instanceof MethodTree)) {
          continue;
        }
        MethodTree methodTree = (MethodTree) memberTree;
        Name methodName = methodTree.getName();

        firstOccurrences.computeIfAbsent(methodName, __ -> methodTree);
      }
      return ImmutableMap.copyOf(firstOccurrences);
    }

    /**
     * Returns a mapping from a method name to the last grouped AST node matching this name.
     *
     * <p>For a class with methods {@code A}, {@code B} and {@code C} in the following order the
     * marked nodes are considered to be "last grouped occurrences":
     *
     * <pre>
     * AABBBABCCAB
     *  ^  ^   ^
     * </pre>
     */
    private ImmutableMap<Name, MethodTree> getLastGroupedOccurrences() {
      Map<Name, Integer> lastGroupedOccurrences = new LinkedHashMap<>();
      for (int i = 0; i < classMembers.size(); i++) {
        Tree memberTree = classMembers.get(i);
        if (!(memberTree instanceof MethodTree)) {
          continue;
        }
        MethodTree methodTree = (MethodTree) memberTree;
        Name methodName = methodTree.getName();

        Integer lastGroupedOccurrence = lastGroupedOccurrences.get(methodName);
        if (lastGroupedOccurrence == null || lastGroupedOccurrence + 1 == i) {
          lastGroupedOccurrences.put(methodName, i);
        }
      }
      return transformMap(lastGroupedOccurrences, (index) -> (MethodTree) classMembers.get(index));
    }
  }

  /**
   * Transforms each value in the given {@code input} map using the {@code mapper} function.
   *
   * @return a new map with transformed values
   */
  private static <K, V1, V2> ImmutableMap<K, V2> transformMap(
      Map<K, V1> input, Function<? super V1, ? extends V2> mapper) {
    return input
        .entrySet()
        .stream()
        .collect(toImmutableMap(Map.Entry::getKey, entry -> mapper.apply(entry.getValue())));
  }

  private static String getMethodFixMessage() {
    return "Overloaded versions of this method are not grouped together";
  }

  private static String getClassFixMessage(Collection<Name> methodNames) {
    String methods =
        methodNames
            .stream()
            .map(methodName -> String.format("\"%s\"", methodName.toString()))
            .sorted()
            .collect(Collectors.joining(", "));
    return String.format("Overloaded methods (%s) of this class are not grouped together", methods);
  }
}
