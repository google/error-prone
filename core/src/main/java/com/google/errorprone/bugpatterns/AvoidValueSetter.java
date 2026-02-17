/*
 * Copyright 2026 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.common.base.Ascii.toUpperCase;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodInvocation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getEnclosedElements;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.regex.Pattern;

/** A bugpattern; see the summary. */
@BugPattern(
    summary =
        "Prefer using the enum-accepting rather than the int-accepting setter for enum fields.",
    severity = WARNING)
public final class AvoidValueSetter extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    String name = symbol.getSimpleName().toString();
    if (!isSubtype(symbol.owner.type, MESSAGE_BUILDER.get(state), state)
        || !name.endsWith("Value")) {
      return NO_MATCH;
    }
    var matcher = PREFIX.matcher(name);
    if (!matcher.find()) {
      return NO_MATCH;
    }
    String fieldName = matcher.group(2);

    Type protoBuilderType = symbol.owner.type;
    Type protoType = symbol.owner.owner.type;

    String fieldNumberMemberNameCamel = toUpperCase(fieldName) + "_FIELD_NUMBER";
    if (protoType != null
        && getEnclosedElements(protoType.tsym).stream()
            .anyMatch(member -> member.getSimpleName().contentEquals(fieldNumberMemberNameCamel))) {
      return NO_MATCH;
    }

    int argIndex;
    if ((name.startsWith("set") || name.startsWith("add")) && tree.getArguments().size() == 1) {
      argIndex = 0;
    } else if (name.startsWith("put") && tree.getArguments().size() == 2) {
      argIndex = 1;
    } else {
      return NO_MATCH;
    }

    ExpressionTree arg = tree.getArguments().get(argIndex);
    var numericValue = constValue(arg, Integer.class);
    if (numericValue == null) {
      return NO_MATCH;
    }

    String enumSetterName = name.substring(0, name.length() - "Value".length());

    for (Symbol member : protoBuilderType.tsym.members().getSymbols()) {
      if (member instanceof MethodSymbol method
          && method.name.contentEquals(enumSetterName)
          && method.getParameters().size() == tree.getArguments().size()
          && isSubtype(
              method.getParameters().get(argIndex).type, PROTOCOL_MESSAGE_ENUM.get(state), state)) {
        return fix(
            tree,
            arg,
            numericValue,
            state,
            method.getParameters().get(argIndex).type,
            enumSetterName);
      }
    }

    return NO_MATCH;
  }

  private Description fix(
      MethodInvocationTree tree,
      ExpressionTree arg,
      int numericValue,
      VisitorState state,
      Type enumType,
      String enumSetterName) {
    return findEnumConstant(enumType, numericValue)
        .map(
            enumConst -> {
              SuggestedFix.Builder fixBuilder =
                  SuggestedFix.builder().merge(renameMethodInvocation(tree, enumSetterName, state));
              fixBuilder.replace(
                  arg, format("%s.%s", qualifyType(state, fixBuilder, enumType.tsym), enumConst));
              return describeMatch(tree, fixBuilder.build());
            })
        .orElse(NO_MATCH);
  }

  private static Optional<String> findEnumConstant(Type enumType, int numericValue) {
    return getEnclosedElements(enumType.tsym).stream()
        .filter(
            symbol ->
                symbol instanceof VarSymbol vs
                    && vs.getConstantValue() instanceof Integer i
                    && i == numericValue
                    && symbol.getSimpleName().toString().endsWith("_VALUE"))
        .map(symbol -> symbol.getSimpleName().toString().replaceAll("_VALUE$", ""))
        .findFirst();
  }

  private static final Pattern PREFIX = Pattern.compile("^(set|add|put)(.+)$");

  private static final Supplier<Type> MESSAGE_BUILDER =
      memoize(state -> state.getTypeFromString("com.google.protobuf.Message.Builder"));

  private static final Supplier<Type> PROTOCOL_MESSAGE_ENUM =
      memoize(state -> state.getTypeFromString("com.google.protobuf.ProtocolMessageEnum"));
}
