/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker} that detects use of the unsafe JNDI API system. */
@BugPattern(
    summary =
        "Using JNDI may deserialize user input via the `Serializable` API which is extremely"
            + " dangerous",
    severity = SeverityLevel.ERROR)
public final class BanJNDI extends BugChecker implements MethodInvocationTreeMatcher {

  /** Checks for direct or indirect calls to context.lookup() via the JDK */
  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(
          anyMethod()
              .onDescendantOf("javax.naming.directory.DirContext")
              .namedAnyOf(
                  "modifyAttributes",
                  "getAttributes",
                  "search",
                  "getSchema",
                  "getSchemaClassDefinition"),
          anyMethod()
              .onDescendantOf("javax.naming.Context")
              .namedAnyOf("lookup", "bind", "rebind", "createSubcontext"),
          anyMethod()
              .onDescendantOf("javax.jdo.JDOHelperTest")
              .namedAnyOf(
                  "testGetPMFBadJNDI",
                  "testGetPMFBadJNDIGoodClassLoader",
                  "testGetPMFNullJNDI",
                  "testGetPMFNullJNDIGoodClassLoader"),
          anyMethod().onDescendantOf("javax.jdo.JDOHelper").named("getPersistenceManagerFactory"),
          anyMethod()
              .onDescendantOf("javax.management.remote.JMXConnectorFactory")
              .named("connect"),
          anyMethod().onDescendantOf("javax.sql.rowset.spi.SyncFactory").named("getInstance"),
          anyMethod()
              .onDescendantOf("javax.management.remote.rmi.RMIConnector.RMIClientCommunicatorAdmin")
              .named("doStart"),
          anyMethod().onDescendantOf("javax.management.remote.rmi.RMIConnector").named("connect"),
          anyMethod().onDescendantOf("javax.naming.InitialContext").named("doLookup"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget() || !MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);

    return description.build();
  }
}
