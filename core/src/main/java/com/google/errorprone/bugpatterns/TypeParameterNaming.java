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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.names.NamingConventions;
import com.sun.source.tree.TypeParameterTree;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** Enforces type parameters match the google style guide. */
@BugPattern(
  name = "TypeParameterNaming",
  summary =
      "Type parameters must be a single letter with an optional numeric suffix,"
          + " or an UpperCamelCase name followed by the letter 'T'.",
  category = JDK,
  severity = SUGGESTION,
  tags = StandardTags.STYLE,
  linkType = LinkType.CUSTOM,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,
  link = "https://google.github.io/styleguide/javaguide.html#s5.2.8-type-variable-names"
)
public class TypeParameterNaming extends BugChecker implements TypeParameterTreeMatcher {

  private static final Pattern SINGLE_PLUS_MAYBE_DIGIT = Pattern.compile("[A-Z]\\d?");

  private static String upperCamelToken(String s) {
    return "" + Ascii.toUpperCase(s.charAt(0)) + (s.length() == 1 ? "" : s.substring(1));
  }

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    if (matchesTypeParameterNamingScheme(tree.getName())) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "Type Parameter %s must be a single letter with an optional numeric"
                    + " suffix, or an UpperCamelCase name followed by the letter 'T'.",
                tree.getName()))
        .addFix(
            TypeParameterShadowing.renameTypeVariable(
                tree,
                state.getPath().getParentPath().getLeaf(),
                replacementName(tree.getName().toString()),
                state))
        .build();
  }

  static boolean matchesTypeParameterNamingScheme(Name name) {
    return SINGLE_PLUS_MAYBE_DIGIT.matcher(name).matches() || matchesClassWithT(name.toString());
  }

  private static String replacementName(String identifier) {
    Preconditions.checkArgument(!identifier.isEmpty());

    // Some early checks:
    // TFoo => FooT
    if (identifier.length() > 2
        && identifier.charAt(0) == 'T'
        && Ascii.isUpperCase(identifier.charAt(1))
        && Ascii.isLowerCase(identifier.charAt(2))) {
      // splitToLowercaseTerms thinks "TFooBar" is ["tfoo", "bar"], so we remove "t", have it parse
      // as ["foo", "bar"], then staple "t" back on the end.
      ImmutableList<String> tokens =
          NamingConventions.splitToLowercaseTerms(identifier.substring(1));
      return Streams.concat(tokens.stream(), Stream.of("T"))
          .map(TypeParameterNaming::upperCamelToken)
          .collect(Collectors.joining());
    }

    ImmutableList<String> tokens = NamingConventions.splitToLowercaseTerms(identifier);

    // UPPERCASE => UppercaseT
    if (tokens.size() == 1) {
      String token = tokens.get(0);
      if (token.toUpperCase().equals(identifier)) {
        return upperCamelToken(token) + "T";
      }
    }

    // FooType => FooT
    if (Iterables.getLast(tokens).equals("type")) {
      return Streams.concat(tokens.subList(0, tokens.size() - 1).stream(), Stream.of("T"))
          .map(TypeParameterNaming::upperCamelToken)
          .collect(Collectors.joining());
    }

    return identifier + "T";
  }

  @VisibleForTesting
  static boolean matchesClassWithT(String identifier) {
    if (!identifier.endsWith("T")) {
      return false;
    }

    ImmutableList<String> tokens = NamingConventions.splitToLowercaseTerms(identifier);
    // Combine the tokens back into UpperCamelTokens and make sure it matches the identifier
    String reassembled =
        tokens.stream().map(TypeParameterNaming::upperCamelToken).collect(Collectors.joining());

    return identifier.equals(reassembled);
  }
}
