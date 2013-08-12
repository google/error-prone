package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneScanner;
import com.google.errorprone.Scanner;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressable;
import com.sun.source.tree.Tree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A base class for implementing bug checkers. The {@code BugChecker} can supply a Scanner
 * implementation for this checker, making it easy to use a single checker.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker implements Suppressable {

  protected final String canonicalName;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Set<String> allNames;
  protected final BugPattern pattern;

  public BugChecker() {
    pattern = this.getClass().getAnnotation(BugPattern.class);
    if (pattern == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    canonicalName = pattern.name();
    allNames = new HashSet<String>();
    allNames.add(canonicalName);
    allNames.addAll(Arrays.asList(pattern.altNames()));
  }

  /**
   * Helper to create a Description for the common case where the diagnostic message is not
   * parameterized.
   */
  protected Description describeMatch(Tree node, SuggestedFix fix) {
    return new Description(node, getDiagnosticMessage(), fix, pattern.severity());
  }

  /**
   * Generate the compiler diagnostic message based on information in the @BugPattern annotation.
   *
   * <p>If the formatSummary element of the annotation has been set, then use format string
   * substitution to generate the message.  Otherwise, just use the summary element directly.
   *
   * @param args Arguments referenced by the format specifiers in the annotation's formatSummary
   *     element
   * @return The compiler diagnostic message.
   */
  protected String getDiagnosticMessage(Object... args) {
    String summary;
    if (!pattern.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(pattern.formatSummary(), args);
    } else {
      summary = pattern.summary();
    }
    return "[" + pattern.name() + "] " + summary + getLink();
  }

  /**
   * Construct the link text to include in the compiler error message.
   */
  private String getLink() {
    switch (pattern.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + pattern.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + pattern.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + pattern.linkType());
    }
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  @Override
  public Set<String> getAllNames() {
    return allNames;
  }

  public final Scanner createScanner() {
    return ErrorProneScanner.forMatcher(this.getClass());
  }
}