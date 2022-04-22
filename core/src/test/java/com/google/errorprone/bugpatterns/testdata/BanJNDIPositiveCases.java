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

package com.google.errorprone.bugpatterns.testdata;

import java.io.IOException;
import java.util.Hashtable;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;

/**
 * {@link BanJNDITest}
 *
 * @author tshadwell@google.com (Thomas Shadwell)
 */
class BanJNDIPositiveCases {
  private static DirContext FakeDirContext = ((DirContext) new Object());

  private void callsModifyAttributes() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.modifyAttributes(((Name) new Object()), 0, ((Attributes) new Object()));
  }

  private void callsGetAttributes() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.getAttributes(((Name) new Object()));
  }

  private void callsSearch() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.search(((Name) new Object()), ((Attributes) new Object()));
  }

  private void callsGetSchema() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.getSchema(((Name) new Object()));
  }

  private void callsGetSchemaClassDefinition() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.getSchemaClassDefinition(((Name) new Object()));
  }

  private static Context FakeContext = ((Context) new Object());

  private void callsLookup() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeContext.lookup("hello");
  }

  private void callsSubclassLookup() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.lookup("hello");
  }

  private void callsBind() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeContext.bind(((Name) new Object()), new Object());
  }

  private void subclassCallsBind() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.bind(((Name) new Object()), new Object());
  }

  private void callsRebind() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeContext.rebind(((Name) new Object()), new Object());
  }

  private void subclassCallsRebind() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.rebind(((Name) new Object()), new Object());
  }

  private void callsCreateSubcontext() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeContext.createSubcontext((Name) new Object());
  }

  private void subclassCallsCreateSubcontext() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    FakeDirContext.createSubcontext((Name) new Object());
  }

  RMIConnector fakeRMIConnector = ((RMIConnector) new Object());

  private void callsRMIConnect() throws IOException {
    // BUG: Diagnostic contains: BanJNDI
    fakeRMIConnector.connect();
  }

  private void callsEnumerateBindings() throws SyncFactoryException {
    // BUG: Diagnostic contains: BanJNDI
    SyncFactory.getInstance("fear is the little-death");
  }

  // unable to load javax.jdo for testing (must be some super optional pkg?)

  private void callsJMXConnectorFactoryConnect() throws IOException {
    // BUG: Diagnostic contains: BanJNDI
    JMXConnectorFactory.connect(((JMXServiceURL) new Object()));
  }

  private void callsDoLookup() throws NamingException {
    // BUG: Diagnostic contains: BanJNDI
    InitialContext.doLookup(((Name) new Object()));
  }

  private static boolean callToJMXConnectorFactoryConnect()
      throws java.net.MalformedURLException, java.io.IOException {
    JMXConnector connector =
        // BUG: Diagnostic contains: BanJNDI
        JMXConnectorFactory.connect(
            new JMXServiceURL("service:jmx:rmi:///jndi/rmi:// fake data 123 "));
    connector.connect();

    return false;
  }

  private Object subclassesJavaNamingcontext() throws NamingException {
    InitialContext c = new InitialContext(new Hashtable(0));
    // BUG: Diagnostic contains: BanJNDI
    return c.lookup("hello");
  }
}
