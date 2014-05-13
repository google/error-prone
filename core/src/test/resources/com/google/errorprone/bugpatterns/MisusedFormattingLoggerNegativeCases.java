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
