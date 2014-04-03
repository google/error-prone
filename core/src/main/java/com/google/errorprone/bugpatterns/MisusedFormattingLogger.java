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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;

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
import java.util.Collections;
import java.util.EnumMap;
import java.util.FormatFlagsConversionMismatchException;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.type.TypeKind;

/**
 * @author vidarh@google.com (Will Holen)
 */
@BugPattern(name = "MisusedFormattingLogger",
    summary = "FormattingLogger uses wrong or mismatched format string",
    explanation = "FormattingLogger is easily misused. There are several similar but "
        + "incompatible methods. Some use printf style formats, some use MessageFormat. "
        + "Some have an optional exception first, some have it last. Failing to pick the "
        + "right method will cause logging information to be lost, or the log call to "
        + "fail at runtime -- often during an error condition when you need it most.",
    category = JDK, maturity = MATURE, severity = ERROR)

public class MisusedFormattingLogger extends BugChecker implements MethodInvocationTreeMatcher {

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> isFormattingLogger = anyOf(
      methodSelect(Matchers.methodReceiver(
          Matchers.isSubtypeOf("com.google.common.logging.FormattingLogger"))),
      methodSelect(Matchers.methodReceiver(
          Matchers.isSubtypeOf("com.google.gdata.util.common.logging.FormattingLogger"))));

  private static final Matcher<Tree> isThrowable =
      isSubtypeOf("java.lang.Throwable");

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> isThrowableMessage =
      methodSelect(anyOf(
          isDescendantOfMethod("java.lang.Throwable", "getMessage()"),
          isDescendantOfMethod("java.lang.Throwable", "toString()")));

  /**
   * A regex pattern for matching logging methods in FormattingLogger.
   *
   * This class has a combinatorial number of methods derived from
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
      "^(?:severe|warning|info|config|fine|finer|finest|log)(fmt)?\\("
      + "(java\\.util\\.logging\\.Level,)?"
      + "(java\\.lang\\.Throwable,)?"
      + "java\\.lang\\.String,java\\.lang\\.Object\\.\\.\\.\\)");

  private static final int FORMAT_GROUP = 1, LEVELPARAM_GROUP = 2, THROWABLEPARAM_GROUP = 3;

  private static final Pattern messageFormatGroup =
      Pattern.compile("\\{ *(\\d+).*?\\}");

  /** A pattern matching a literal region, <strong>after</strong> literal single quote removal. */
  private static final Pattern literalRegion = Pattern.compile("'[^']*'?");

  /** Regex that matches printf groups, copied from
   * edu.umd.cs.findbugs.formatStringChecker.Formatter. */
  private static final Pattern printfGroup =
      Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

  private static final Map<TypeKind, String> BOXED_TYPE_NAMES;

  static {
    Map<TypeKind, String> boxedTypeNames = new EnumMap<TypeKind, String>(TypeKind.class);
    boxedTypeNames.put(TypeKind.BYTE, Byte.class.getName());
    boxedTypeNames.put(TypeKind.SHORT, Short.class.getName());
    boxedTypeNames.put(TypeKind.INT, Integer.class.getName());
    boxedTypeNames.put(TypeKind.LONG, Long.class.getName());
    boxedTypeNames.put(TypeKind.FLOAT, Float.class.getName());
    boxedTypeNames.put(TypeKind.DOUBLE, Double.class.getName());
    boxedTypeNames.put(TypeKind.BOOLEAN, Boolean.class.getName());
    boxedTypeNames.put(TypeKind.CHAR, Character.class.getName());
    boxedTypeNames.put(TypeKind.NULL, Object.class.getName());
    BOXED_TYPE_NAMES = Collections.unmodifiableMap(boxedTypeNames);
  }

  // get type name in format accepted by Formatter.check
  private static String getFormatterType(Type type) {
    String boxedTypeName = BOXED_TYPE_NAMES.get(type.getKind());
    String typeName = (boxedTypeName != null ? boxedTypeName : type.toString());
    return ("L" + typeName.replace(".", "/") + ";");
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    FormatParameters parameters = findFormatParameters(tree, state);
    if (parameters == null) {
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

    List<String> errors = new ArrayList<String>();
    int formatIndex = parameters.getFormatIndex();
    List<? extends ExpressionTree> args = tree.getArguments();
    JCLiteral format = (JCLiteral) args.get(formatIndex);
    String formatString = (String) format.getValue();

    List<ExpressionTree> leadingArguments = new ArrayList<ExpressionTree>();
    List<ExpressionTree> formatArguments = new ArrayList<ExpressionTree>();

    for (int i = 0; i < args.size(); i++) {
      if (i < formatIndex) {
        leadingArguments.add(args.get(i));
      } else if (i > formatIndex) {
        formatArguments.add(args.get(i));
      }
    }

    // Automatically rewrite if this is the wrong type of format string
    String rewrite = ensureFormatStringType(parameters.getType(), formatString);

    if (!rewrite.equals(formatString)) {
        errors.add("uses the wrong format style");
        formatString = rewrite;
    } else {
      // If we're using the user's string, ensure it's valid.
      Exception formatException = null;
      try {
        if (parameters.getType() == FormatType.MESSAGEFORMAT) {
          new MessageFormat(formatString);
        } else if (parameters.getType() == FormatType.PRINTF) {
          verifyPrintf(tree, parameters);
        }
      // Due to Java 1.6, we have to write these out:
      } catch (IllegalArgumentException e) {
        formatException = e;
      } catch (FormatterException e) {
        formatException = e;
      }
      if (formatException != null) {
        return new Description(tree,
            "Format string is invalid: " + formatException.getMessage(),
            SuggestedFix.NO_FIX, pattern.severity());
      }
    }

    // Are there format string references that aren't provided?
    Set<Integer> referencedArguments = getReferencedArguments(parameters.getType(), formatString);
    if (referencesUnspecifiedArguments(referencedArguments, formatArguments.size())) {
      return describeMatch(tree, SuggestedFix.NO_FIX);
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
          errors.add("has arguments only referenced in literal sections");
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
      List<String> additionalArgs = new ArrayList<String>();
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

    if (errors.size() > 0) {
      List<String> newParameters = new ArrayList<String>();
      for (ExpressionTree t : leadingArguments) {
        newParameters.add(t.toString());
      }
      newParameters.add(makeLiteral(state, formatString));
      for (ExpressionTree t : formatArguments) {
        newParameters.add(t.toString());
      }

      int sourceStart = ((JCTree) args.get(0)).getStartPosition();
      int sourceEnd = state.getEndPosition((JCTree) args.get(args.size() - 1));

      SuggestedFix fix = new SuggestedFix().replace(sourceStart, sourceEnd,
          join(", ", newParameters));
       return new Description(tree,
           "This call " + join(", ", errors) + ".", fix, pattern.severity());
    }

    return Description.NO_MATCH;
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

    List<String> argTypes = new ArrayList<String>();
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

    Set<Integer> set = new HashSet<Integer>();
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

    Set<Integer> references = new HashSet<Integer>();
    while (matcher.find()) {
      references.add(Integer.parseInt(matcher.group(1)));
    }
    return references;
  }

  private int max(Set<Integer> set) {
    int max = -1;
    for (int i : set) {
      max = Math.max(i, max);
    }
    return max;
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

  private boolean hasUnmatchedQuotes(String string) {
    return string.replaceAll("[^']", "").length() % 2 == 1;
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

        return parameters;
      }
    }
    return null;
  }

  private String makeLiteral(VisitorState state, String value) {
    return TreeMaker.instance(state.context).Literal(value).toString()
        .replaceAll("\\\\'", "'"); // Humans wouldn't escape ' inside double quotes.
  }

  private String ensureFormatStringType(FormatType type, String formatString) {
    if (type == FormatType.MESSAGEFORMAT) {
      if (!mayBeMessageFormat(formatString) && mayBePrintfFormat(formatString)) {
        return printfToMessageFormat(formatString);
      }
    } else if (type == FormatType.PRINTF) {
      if (!mayBePrintfFormat(formatString) && mayBeMessageFormat(formatString)) {
        return messageFormatToPrintf(formatString);
      }
    }
    return formatString;
  }

  private boolean mayBePrintfFormat(String formatString) {
    return printfGroup.matcher(formatString).find();
  }

  private boolean mayBeMessageFormat(String formatString) {
    return messageFormatGroup.matcher(formatString).find();
  }

  /** Attempt to convert a printf format string into a MessageFormat one.
   * This is far from perfect. It doesn't handle indexed printf parameters, ignores type
   * conversion, loses formatting, may mistrigger for escaped values and doesn't
   * care about number of arguments.
   *
   * However, the fix is effective most of the time (since most formats are very basic),
   * and in any case it will be an improvement over what the user currently does.
   */
  private String printfToMessageFormat(String printfString) {
    java.util.regex.Matcher matcher = printfGroup.matcher(printfString);

    StringBuilder result = new StringBuilder();
    int parameterIndex = 0;
    int lastPosition = 0;
    while (matcher.find()) {
      result.append(printfString.substring(lastPosition, matcher.start()));
      result.append("{").append(parameterIndex).append("}");
      lastPosition = matcher.end();
      parameterIndex++;
    }
    result.append(printfString.substring(lastPosition, printfString.length()));
    return result.toString().replaceAll("'", "''");
  }

  private String messageFormatToPrintf(String messageFormat) {
    return messageFormatGroup.matcher(messageFormat).replaceAll("%s");
  }

  private static class FormatParameters {
    private FormatType type;
    private int formatIndex;

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
  }

  static enum FormatType {
    PRINTF,
    MESSAGEFORMAT
  }
}
