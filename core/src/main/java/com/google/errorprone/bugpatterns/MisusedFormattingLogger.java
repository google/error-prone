/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.isPrimitiveArrayType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.TreeMaker;

import edu.umd.cs.findbugs.formatStringChecker.ExtraFormatArgumentsException;
import edu.umd.cs.findbugs.formatStringChecker.Formatter;
import edu.umd.cs.findbugs.formatStringChecker.FormatterException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.FormatFlagsConversionMismatchException;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.type.TypeKind;

/**
 * @author vidarh@google.com (Will Holen)
 */
@BugPattern(name = "MisusedFormattingLogger",
    summary = "FormattingLogger uses wrong or mismatched format string",
    explanation = "FormattingLogger is easily misused. There are several similar but "
        + "incompatible methods.  Methods ending in \"fmt\" use String.format, but the "
        + "corresponding methods without that suffix use MessageFormat. Some methods have an "
        + "optional exception first, and some have it last. Failing to pick "
        + "the right method will cause logging information to be lost or the log call to "
        + "fail at runtime -- often during an error condition when you need it most.\n\n"
        + "There are further gotchas.  For example, MessageFormat strings cannot "
        + "have unbalanced single quotes (e.g., \"Don't log {0}\" will not format {0} because "
        + "of the quote in \"Don't\"). The number of format elements must match the number of "
        + "arguments provided, and for String.format, the types must match as well.  And so on.",
    category = JDK, maturity = MATURE, severity = WARNING)

public class MisusedFormattingLogger extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> isFormattingLogger = anyOf(
      methodSelect(Matchers.methodReceiver(
          Matchers.isSubtypeOf("com.google.common.logging.FormattingLogger"))),
      methodSelect(Matchers.methodReceiver(
          Matchers.isSubtypeOf("com.google.gdata.util.common.logging.FormattingLogger"))));

  private static final Matcher<Tree> isThrowable =
      isSubtypeOf("java.lang.Throwable");

  private static final Matcher<MethodInvocationTree> isThrowableMessage =
      methodSelect(Matchers.<ExpressionTree>anyOf(
          isDescendantOfMethod("java.lang.Throwable", "getMessage()"),
          isDescendantOfMethod("java.lang.Throwable", "toString()")));

  /**
   * A regex pattern for matching logging methods in FormattingLogger.
   *
   * <p>This class has a combinatorial number of methods derived from
   * <ol>
   *   <li>A log level name, or generically "log"</li>
   *   <li>A format type ("fmt" for printf, nothing for MessageFormat)</li>
   *   <li>A Level parameter if not specified by the method name</li>
   *   <li>Zero or One Throwable parameter</li>
   *   <li>The format string and parameters</li>
   * </ol>
   *
   * Examples of this includes <code>severe(String, Object...)</code> and
   * <code>infofmt(Throwable, String, Object...)</code>. This pattern matches them all.
   */
  private static final Pattern formattingLoggerMethods = Pattern.compile(
      // Due to Java 1.6 we can't use named groups, so there are constants instead
      "^(severe|warning|info|config|fine|finer|finest|log)(fmt)?\\("
      + "(java\\.util\\.logging\\.Level,)?"
      + "(java\\.lang\\.Throwable,)?"
      + "java\\.lang\\.String,java\\.lang\\.Object\\.\\.\\.\\)");

  private static final int
      BASE_GROUP = 1, FORMAT_GROUP = 2, LEVELPARAM_GROUP = 3, THROWABLEPARAM_GROUP = 4;

  private static final Pattern messageFormatGroup =
      Pattern.compile("\\{ *(\\d+).*?\\}");

  /** A pattern matching a literal region, <strong>after</strong> literal single quote removal. */
  private static final Pattern literalRegion = Pattern.compile("'[^']*'?");

  /** Regex that matches printf groups, copied from
   * edu.umd.cs.findbugs.formatStringChecker.Formatter. */
  private static final Pattern printfGroup =
      Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

  private static final ImmutableMap<TypeKind, String> BOXED_TYPE_NAMES;

  static {
    EnumMap<TypeKind, String> boxedTypeNames = new EnumMap<>(TypeKind.class);
    boxedTypeNames.put(TypeKind.BYTE, Byte.class.getName());
    boxedTypeNames.put(TypeKind.SHORT, Short.class.getName());
    boxedTypeNames.put(TypeKind.INT, Integer.class.getName());
    boxedTypeNames.put(TypeKind.LONG, Long.class.getName());
    boxedTypeNames.put(TypeKind.FLOAT, Float.class.getName());
    boxedTypeNames.put(TypeKind.DOUBLE, Double.class.getName());
    boxedTypeNames.put(TypeKind.BOOLEAN, Boolean.class.getName());
    boxedTypeNames.put(TypeKind.CHAR, Character.class.getName());
    boxedTypeNames.put(TypeKind.NULL, Object.class.getName());
    BOXED_TYPE_NAMES = Maps.immutableEnumMap(boxedTypeNames);
  }

  // get type name in format accepted by Formatter.check
  private static String getFormatterType(Type type) {
    String boxedTypeName = BOXED_TYPE_NAMES.get(type.getKind());
    String typeName = (boxedTypeName != null ? boxedTypeName : type.toString());
    return ("L" + typeName.replace('.', '/') + ";");
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    FormatParameters parameters = findFormatParameters(tree, state);
    if (parameters == null) {
      return Description.NO_MATCH;
    }

    if (!isVariadicInvocation(tree, state, parameters)) {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> args = tree.getArguments();
    if (args.get(parameters.getFormatIndex()).getKind() != Kind.STRING_LITERAL) {
      return Description.NO_MATCH;
    }

    try {
      return checkFormatString(tree, state, parameters);
    } catch (UnsupportedOperationException e) {
      return Description.NO_MATCH;
    }
  }

  private Description checkFormatString(
      MethodInvocationTree tree, VisitorState state, FormatParameters parameters) {

    List<String> errors = new ArrayList<>();
    int formatIndex = parameters.getFormatIndex();
    List<? extends ExpressionTree> args = tree.getArguments();
    JCLiteral format = (JCLiteral) args.get(formatIndex);
    String formatString = (String) format.getValue();

    List<ExpressionTree> leadingArguments = new ArrayList<>();
    List<ExpressionTree> formatArguments = new ArrayList<>();

    for (int i = 0; i < args.size(); i++) {
      if (i < formatIndex) {
        leadingArguments.add(args.get(i));
      } else if (i > formatIndex) {
        formatArguments.add(args.get(i));
      }
    }

    // Automatically switch method if this is the wrong type of format string
    boolean rewriteMethod = changeFormatTypeIfRequired(parameters, formatString);
    if (rewriteMethod) {
        errors.add("uses the wrong method for the format type");
    }

    Exception formatException = null;
    try {
      if (parameters.getType() == FormatType.MESSAGEFORMAT) {
        new MessageFormat(formatString);
      } else if (parameters.getType() == FormatType.PRINTF) {
        verifyPrintf(tree, parameters);
      }
    } catch (Exception e) {
      formatException = e;
    }
    if (formatException != null) {
      String customMessage = "Format string is invalid";
      if (formatException.getMessage() != null) {
        customMessage += ": " + formatException.getMessage();
      }
      return buildDescription(tree)
          .setMessage(customMessage)
          .build();
    }

    // Are there format string references that aren't provided?
    Set<Integer> referencedArguments = getReferencedArguments(parameters.getType(), formatString);
    if (referencesUnspecifiedArguments(referencedArguments, formatArguments.size())) {
      return describeMatch(tree);
    }

    // Are there parameters that aren't referenced in a MessageFormat?
    if (parameters.getType() == FormatType.MESSAGEFORMAT
        && referencedArguments.size() < formatArguments.size()) {

      // Could it be because the user is unaware of magic single quotes?
      String quotedString = formatString.replace("'", "''");

      if (hasQuotedArguments(formatString)) {
        Set<Integer> updatedReferences =
            getReferencedArguments(parameters.getType(), quotedString);

        if (updatedReferences.size() > referencedArguments.size()) {
          formatString = quotedString;
          errors.add("has arguments masked by single quotes");
          referencedArguments = updatedReferences;
        }
      }

      if (hasUnmatchedQuotes(formatString)) {
        formatString = quotedString;
        errors.add("has unmatched single quotes");
      }
    }

    // Could we reorder a final, unreferenced exception?
    if (parameters.getAllowExceptionReordering()
        && referencedArguments.size() < formatArguments.size()
        && max(referencedArguments) < formatArguments.size() - 1) {

      ExpressionTree last = formatArguments.get(formatArguments.size() - 1);

      if (isThrowable.matches(last, state)) {
        leadingArguments.add(last);
        formatArguments.remove(formatArguments.size() - 1);
        errors.add("ignores the passed exception");
      } else if (last instanceof MethodInvocationTree
          && isThrowableMessage.matches((MethodInvocationTree) last, state)) {
        ExpressionTree target = getInvocationTarget((MethodInvocationTree) last);
        if (target != null) {
          leadingArguments.add(target);
          formatArguments.remove(formatArguments.size() - 1);
          errors.add("ignores the passed exception message");
        }
      }
    }

    // After quoting and reordering, are there any more unreferenced parameters?
    if (referencedArguments.size() < formatArguments.size()) {
      // Then let's add them to the format string
      List<String> additionalArgs = new ArrayList<>();
      for (int i = 0; i < formatArguments.size(); i++) {
        String arg = parameters.getType() == FormatType.MESSAGEFORMAT ?
            "{" + i + "}" : "%s";
        if (!referencedArguments.contains(i)) {
          additionalArgs.add(arg);
        }
      }
      formatString = formatString + " (" + join(", ", additionalArgs) + ")";
      errors.add("ignores some parameters");
    }

    if (!errors.isEmpty()) {
      List<String> newParameters = new ArrayList<>();
      for (ExpressionTree t : leadingArguments) {
        newParameters.add(t.toString());
      }
      newParameters.add(makeLiteral(state, formatString));
      for (ExpressionTree t : formatArguments) {
        newParameters.add(t.toString());
      }

      // logger.method(param1, param2)
      //       ^-- methodStart      ^-- parameterEnd
      int methodStart = state.getEndPosition((JCTree) getInvocationTarget(tree));
      int parameterEnd = state.getEndPosition((JCTree) args.get(args.size() - 1));

      Description.Builder descriptionBuilder = buildDescription(tree)
          .setMessage("This call " + join(", ", errors));
      if (methodStart >= 0 && parameterEnd >= 0) {
        String replacement = "." + parameters.getMethodName() + "(" +  join(", ", newParameters);
        descriptionBuilder.addFix(SuggestedFix.replace(methodStart, parameterEnd, replacement));
      }
      return descriptionBuilder.build();
    }

    return Description.NO_MATCH;
  }

  // Check whether this is call is made variadically or through a final Object array parameter
  private boolean isVariadicInvocation(
      MethodInvocationTree tree, VisitorState state, FormatParameters params) {
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() == params.getFormatIndex() + 2) {
      ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
      return !isArrayType().matches(lastArgument, state)
          || isPrimitiveArrayType().matches(lastArgument, state);
    }
    return true;
  }

  private ExpressionTree getInvocationTarget(MethodInvocationTree invocation) {
    if (invocation.getMethodSelect() instanceof MemberSelectTree) {
      return ((MemberSelectTree) invocation.getMethodSelect()).getExpression();
    }
    return null;
  }

  // Run the FindBugs checker on the string, to catch anything we don't currently detect.
  private void verifyPrintf(MethodInvocationTree tree, FormatParameters parameters)
      throws FormatFlagsConversionMismatchException, IllegalFormatException, FormatterException {
     List<? extends ExpressionTree> args = tree.getArguments();

    JCLiteral format = (JCLiteral) args.get(parameters.getFormatIndex());
    String formatString = (String) format.getValue();

    List<String> argTypes = new ArrayList<>();
    for (int i = parameters.getFormatIndex() + 1; i < args.size(); ++i) {
      Type type = ((JCExpression) args.get(i)).type;
      argTypes.add(getFormatterType(type));
    }

    try {
      Formatter.check(formatString, argTypes.toArray(new String[0]));
    } catch (ExtraFormatArgumentsException e) {
      return; // We can handle this.
    }
  }

  private boolean referencesUnspecifiedArguments(Set<Integer> usedReferences, int argumentCount) {
    for (int i : usedReferences) {
      if (i > argumentCount) {
        return true;
      }
    }
    return false;
  }

  // This is Joiner.on(separator).join(strings) but avoids pulling in Guava Collections
  private static String join(String separator, Iterable<String> strings) {
    StringBuilder builder = new StringBuilder();
    for (String s : strings) {
      builder.append(s).append(separator);
    }
    return builder.length() == 0 ? "" :
        builder.substring(0, builder.length() - separator.length());
  }

  private Set<Integer> getReferencedArguments(FormatType type, String string) {
    if (type == FormatType.PRINTF) {
      return getReferencedArgumentsP(string);
    } else if (type == FormatType.MESSAGEFORMAT) {
      return getReferencedArgumentsM(string);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private Set<Integer> getReferencedArgumentsP(String str) {
    java.util.regex.Matcher matcher = printfGroup.matcher(str);

    Set<Integer> set = new HashSet<>();
    int i = 0;
    while (matcher.find()) {
      // %n is line break and %% is literal percent, they don't reference parameters.
      if (!matcher.group().endsWith("n") && !matcher.group().endsWith("%")) {
        if (matcher.group(1) != null) {
          set.add(Integer.parseInt(matcher.group(1).replaceAll("\\$", "")));
        } else {
          set.add(i);
          i++;
        }
      }
    }
    return set;
  }

  private Set<Integer> getReferencedArgumentsM(String str) {
    str = str.replaceAll("''", "");  // remove literal single quotes
    str = literalRegion.matcher(str).replaceAll("");
    java.util.regex.Matcher matcher = messageFormatGroup.matcher(str);

    Set<Integer> references = new HashSet<>();
    while (matcher.find()) {
      references.add(Integer.parseInt(matcher.group(1)));
    }
    return references;
  }

  private static int max(Collection<Integer> ints) {
    return ints.isEmpty() ? -1 : Collections.max(ints);
  }

  private boolean hasQuotedArguments(String string) {
    string = string.replaceAll("''", "");  // remove literal single quotes
    java.util.regex.Matcher match = literalRegion.matcher(string);
    while (match.find()) {
      if (messageFormatGroup.matcher(match.group()).find()) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasUnmatchedQuotes(String string) {
    return CharMatcher.is('\'').countIn(string) % 2 == 1;
  }

  /** Given a method invocation, find whether this is a supported formatting call,
   * if so which kind and which argument is the format string. */
  private FormatParameters findFormatParameters(
      MethodInvocationTree tree, VisitorState state) {

    FormatParameters parameters = new FormatParameters();

    if (isFormattingLogger.matches(tree, state)) {
      String methodName = ASTHelpers.getSymbol(tree).toString();
      java.util.regex.Matcher matcher = formattingLoggerMethods.matcher(methodName);

      if (matcher.matches()) {
        int formatIndex = 0;
        if (matcher.group(LEVELPARAM_GROUP) != null) {
          formatIndex++;
        }
        if (matcher.group(THROWABLEPARAM_GROUP) != null) {
          formatIndex++;
        } else {
          parameters.setAllowExceptionReordering(true);
        }

        parameters.setFormatIndex(formatIndex);
        if (matcher.group(FORMAT_GROUP) == null) {
          parameters.setType(FormatType.MESSAGEFORMAT);
        } else {
          parameters.setType(FormatType.PRINTF);
        }

        parameters.setMethodBase(matcher.group(BASE_GROUP));
        return parameters;
      }
    }
    return null;
  }

  private String makeLiteral(VisitorState state, String value) {
    return TreeMaker.instance(state.context).Literal(value).toString()
        .replaceAll("\\\\'", "'"); // Humans wouldn't escape ' inside double quotes.
  }

  /** Change the FormattingParameters to a different format type if the format string
   * doesn't match the method's expected input. Returns true if a change was made. */
  private boolean changeFormatTypeIfRequired(FormatParameters parameters, String formatString) {
    if (parameters.getType() == FormatType.MESSAGEFORMAT
        && !mayBeMessageFormat(formatString) && mayBePrintfFormat(formatString)) {
      parameters.setType(FormatType.PRINTF);
      return true;
    }
    if (parameters.getType() == FormatType.PRINTF
        && !mayBePrintfFormat(formatString) && mayBeMessageFormat(formatString)) {
      parameters.setType(FormatType.MESSAGEFORMAT);
      return true;
    }
    return false;
  }

  private boolean mayBePrintfFormat(String formatString) {
    return printfGroup.matcher(formatString).find();
  }

  private boolean mayBeMessageFormat(String formatString) {
    return messageFormatGroup.matcher(formatString).find();
  }

  private static class FormatParameters {
    private FormatType type;
    private int formatIndex;
    private String methodBase;

    // If the last parameter is an exception, can we move it?
    private boolean allowExceptionReordering;

    int getFormatIndex() {
      return formatIndex;
    }

    FormatParameters setFormatIndex(int formatIndex) {
      this.formatIndex = formatIndex;
      return this;
    }

    boolean getAllowExceptionReordering() {
      return allowExceptionReordering;
    }

    FormatParameters setAllowExceptionReordering(boolean allow) {
      allowExceptionReordering = allow;
      return this;
    }

    FormatType getType() {
      return type;
    }

    FormatParameters setType(FormatType type) {
      this.type = type;
      return this;
    }

    String getMethodBase() {
      return methodBase;
    }

    FormatParameters setMethodBase(String methodBase) {
      this.methodBase = methodBase;
      return this;
    }

    String getMethodName() {
      return getMethodBase() + (type == FormatType.PRINTF ? "fmt" : "");
    }
  }

  static enum FormatType {
    PRINTF,
    MESSAGEFORMAT
  }
}
