/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** Flag DateFormats which use the same field more than once. */
@BugPattern(summary = "Reuse of DateFormat fields is most likely unintentional", severity = WARNING)
public final class DuplicateDateFormatField extends MisusedDateFormat {
  private static final ImmutableSet<Character> PATTERN_CHARACTERS =
      ImmutableSet.of(
          'G', 'y', 'Y', 'M', 'L', 'w', 'W', 'D', 'd', 'F', 'E', 'u', 'a', 'H', 'k', 'K', 'h', 'm',
          's', 'S', 'z', 'Z', 'X');

  private static class PatternCounter implements DateFormatConsumer {

    private final Set<Character> seen = new HashSet<>();
    private final Set<Character> duplicates = new HashSet<>();
    @Nullable private Character prev = null;
    private int optionalGroupDepth = 0;

    @Override
    public void consumeSpecial(char special) {
      if (special == '[') {
        optionalGroupDepth++;
      } else if (special == ']') {
        optionalGroupDepth--;
      }
      if (!PATTERN_CHARACTERS.contains(special) || optionalGroupDepth > 0) {
        prev = null;
        return;
      }
      if (prev == null || prev != special) {
        if (!seen.add(special)) {
          duplicates.add(special);
        }
        prev = special;
      }
    }

    @Override
    public void consumeLiteral(char literal) {
      prev = null;
    }

    public Set<Character> getDuplicates() {
      return duplicates;
    }

    public static ImmutableSet<Character> getDuplicates(String pattern) {
      PatternCounter counter = new PatternCounter();
      parseDateFormat(pattern, counter);
      return ImmutableSet.copyOf(counter.getDuplicates());
    }
  }

  @Override
  public Optional<String> rewriteTo(String pattern) {
    return Optional.empty();
  }

  @Override
  Description constructDescription(Tree tree, ExpressionTree patternArg, VisitorState state) {

    Optional<String> pattern = Optional.ofNullable(constValue(patternArg, String.class));
    if (pattern.isEmpty()) {
      return NO_MATCH;
    }
    ImmutableSet<Character> duplicates = PatternCounter.getDuplicates(pattern.get());
    if (!duplicates.isEmpty()) {
      return buildDescription(tree).setMessage(buildMessage(pattern.get(), duplicates)).build();
    }
    return NO_MATCH;
  }

  private static String buildMessage(String pattern, ImmutableSet<Character> duplicates) {
    String duplicatedFields =
        duplicates.stream().sorted().map(c -> "'" + c + "'").collect(joining(", "));
    String fieldDescription =
        duplicates.size() > 1
            ? String.format("the fields [%s]", duplicatedFields)
            : String.format("the field %s", duplicatedFields);
    return String.format(
        "DateFormat pattern \"%s\" uses %s more than once. Reuse of DateFormat fields is most"
            + " likely unintentional.",
        pattern, fieldDescription);
  }
}
