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
