---
title: MisusedFormattingLogger
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

# Bug pattern: MisusedFormattingLogger
__FormattingLogger uses wrong or mismatched format string__

## The problem
FormattingLogger is easily misused. There are several similar but incompatible methods.  Methods ending in "fmt" use String.format, but the corresponding methods without that suffix use MessageFormat. Some methods have an optional exception first, and some have it last. Failing to pick the right method will cause logging information to be lost or the log call to fail at runtime -- often during an error condition when you need it most.

There are further gotchas.  For example, MessageFormat strings cannot have unbalanced single quotes (e.g., "Don't log {0}" will not format {0} because of the quote in "Don't"). The number of format elements must match the number of arguments provided, and for String.format, the types must match as well.  And so on.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MisusedFormattingLogger")` annotation to the enclosing element.

----------

# Examples
__MisusedFormattingLoggerNegativeCases.java__
{% highlight java %}
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


import com.google.gdata.util.common.logging.FormattingLogger;

import java.util.logging.Level;

/**
 * @author vidarh@google.com (Will Holen)
 */
public class MisusedFormattingLoggerNegativeCases {
  private static final FormattingLogger logger =
      FormattingLogger.getLogger(MisusedFormattingLoggerNegativeCases.class);

  public void literals() {
    logger.log(Level.WARNING, "'{42}' is quoted, but {0} is not", "this");
  }

  public void variables() {
    String s = "bar";
    Integer i = 7;
    int j = 8;
    float f = 2.71f;
    Object o = new Object();

    logger.infofmt("%s %d", s, i);
    logger.logfmt(Level.SEVERE, new IllegalArgumentException(), "%f %o", f, o);
    logger.severe("{0}, {1}, {2}", s, i, j);
  }

  public void specialCases() {
    logger.finer("{0} doesn't mind masked {1} if parameters match", "error-prone");
    logger.finerfmt("%1$-3s", "doesn't break indexed printf parameters.");
    logger.finestfmt("%n %%");
    logger.severe("Test", new Object[0]);
    logger.severe("{0} {1}", new Object[] { "a", "b" });
  }
}

{% endhighlight %}
__MisusedFormattingLoggerPositiveCases.java__
{% highlight java %}
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

import com.google.gdata.util.common.logging.FormattingLogger;

import java.util.logging.Level;

/**
 * Positive test cases for MisusedFormattingLogger.
 *
 * @author vidarh@google.com (Will Holen)
 */
public class MisusedFormattingLoggerPositiveCases {
  private static final FormattingLogger logger =
      FormattingLogger.getLogger(MisusedFormattingLoggerPositiveCases.class);

  private Exception getException() {
    return new Exception();
  }

  public void messageFormatLogWouldLoseData() {
    // BUG: Diagnostic contains: logger.warning("{2} and {0} ({1})", "foo", "bar", "baz");
    logger.warning("{2} and {0}", "foo", "bar", "baz");

    // BUG: Diagnostic contains: logger.severe("{1} and {1} ({0}, {2})", "foo", "bar", "baz");
    logger.severe("{1} and {1}", "foo", "bar", "baz");

    // BUG: Diagnostic contains: logger.info("Invalid id ({0})", "id");
    logger.info("Invalid id", "id");
  }

  public void printfLogWouldLoseData() {
    // BUG: Diagnostic contains: logger.warningfmt("%8s and %s (%s)", "foo", "bar", "baz");
    logger.warningfmt("%8s and %s", "foo", "bar", "baz");

    // BUG: Diagnostic contains: logger.infofmt("Invalid id (%s)", "id");
    logger.infofmt("Invalid id", "id");

    // BUG: Diagnostic contains: Format string is invalid: Conversion = 'p'
    logger.infofmt("%p", 0xDEADBEEF);
  }

  public void messageFormatIsAccidentallyQuoted() {
    // BUG: Diagnostic contains: logger.info("User''s id: {0}", 123)
    logger.info("User's id: {0}", 123);

    // BUG: Diagnostic contains: logger.info("Id: ''{0}''", 123)
    logger.info("Id: '{0}'", 123);

    // Make sure tests break if String literals stop unnecessarily escaping single quotes:
    // BUG: Diagnostic contains: logger.severe("User\\''s id: {0}", 123)
    logger.severe("User\\'s id: {0}", 123);
  }

  public void wrongFormatStringType() {
    // BUG: Diagnostic contains: logger.infofmt("User %04.4f requested %s", 3.14, "test")
    logger.info("User %04.4f requested %s", 3.14, "test");

    // BUG: Diagnostic contains: logger.severefmt("Value: '%d'", 42)
    logger.severe("Value: '%d'", 42);

    // BUG: Diagnostic contains: logger.finest("User {0,number} requested {1}", 42, "test")
    logger.finestfmt("User {0,number} requested {1}", 42, "test");
  }

  public void wrongExceptionPosition() {
    // BUG: Diagnostic contains: logger.warning(new RuntimeException("x"), "{0}", 42)
    logger.warning("{0}", 42, new RuntimeException("x"));

    // BUG: Diagnostic contains: logger.log(Level.WARNING, new RuntimeException("x"), "{0}", 42)
    logger.log(Level.WARNING, "{0}", 42, new RuntimeException("x"));

    // BUG: Diagnostic contains: logger.severe(new RuntimeException(), "Error")
    logger.severe("Error", new RuntimeException().toString());

    // BUG: Diagnostic contains: logger.severe(this.getException().getCause(), "Error")
    logger.severe("Error", this.getException().getCause().toString());

    // BUG: Diagnostic contains: logger.warningfmt(new RuntimeException("x"), "%s (%s)", 42, new RuntimeException("y"))
    logger.warningfmt(new RuntimeException("x"), "%s", 42, new RuntimeException("y"));
  }

  public void combo() {
    // BUG: Diagnostic contains: logger.warningfmt(new RuntimeException(), "var='%s' (%s)", "foo", "bar");
    logger.warning("var='%s'", "foo", "bar", new RuntimeException().toString());
  }
}

{% endhighlight %}
