package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Represents an error-prone bug check.  May be composed of multiple matchers that must be matched
 * against different tree node types.
 *
 * <p>Implementations of this class must be annotated with {@link com.google.errorprone.BugPattern}.
 *
 * TODO(eaftan): We should be able to compose multiple BugPatterns into a single error-prone
 * Scanner, and we should have an easy way to generate a Scanner from a single BugChecker.
 * How to reference this Scanner from Refactory?
 * Perhaps this should look like Refactoring?
 */
public abstract class BugChecker {

  protected final String canonicalName;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Collection<String> allNames;
  protected final BugPattern annotation;

  public BugChecker() {
    annotation = this.getClass().getAnnotation(BugPattern.class);
    if (annotation == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    canonicalName = annotation.name();
    allNames = new ArrayList<String>(annotation.altNames().length + 1);
    allNames.add(canonicalName);
    allNames.addAll(Arrays.asList(annotation.altNames()));
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
    if (!annotation.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(annotation.formatSummary(), args);
    } else {
      summary = annotation.summary();
    }
    return "[" + annotation.name() + "] " + summary + getLink();
  }

  /**
   * Construct the link text to include in the compiler error message.
   */
  private String getLink() {
    switch (annotation.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + annotation.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (annotation.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + annotation.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + annotation.linkType());
    }
  }

}
