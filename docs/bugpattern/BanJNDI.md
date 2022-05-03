JNDI ("Java Naming and Directory Interface") is a Java JDK API representing an
abstract directory service such as DNS, a file system or LDAP. Critically, JNDI
allows Java objects to be serialized and deserialized on the wire in
implementing systems. This means that if a Java application is allowed to
perform a JNDI lookup over some transport protocol then the server it connects
to can execute arbitrary attacker-defined code. See
[this](https://www.blackhat.com/docs/us-16/materials/us-16-Munoz-A-Journey-From-JNDI-LDAP-Manipulation-To-RCE.pdf)
Black Hat talk for more information.

This checker bans usage of every API in the Java JDK that can result in
deserialising an unsafe object via JNDI. The list of APIs is generated from
static callgraph analysis of the JDK, rooted at `javax.naming.Context.lookup`
and is as follows:

-   `javax.naming.Context.lookup`
-   `javax.jdo.JDOHelper.getPersistenceManagerFactory`
-   `javax.jdo.JDOHelperTest.testGetPMFBadJNDI`
-   `javax.jdo.JDOHelperTest.testGetPMFBadJNDIGoodClassLoader`
-   `javax.jdo.JDOHelperTest.testGetPMFNullJNDI`
-   `javax.jdo.JDOHelperTest.testGetPMFNullJNDIGoodClassLoader`
-   `javax.management.remote.JMXConnectorFactory.connect`
-   `javax.management.remote.rmi.RMIConnector.connect`
-   `javax.management.remote.rmi.RMIConnector.findRMIServer`
-   `javax.management.remote.rmi.RMIConnector.findRMIServerJNDI`
-   `javax.management.remote.rmi.RMIConnector.RMIClientCommunicatorAdmin.doStart`
-   `javax.naming.directory.InitialDirContext.bind`
-   `javax.naming.directory.InitialDirContext.createSubcontext`
-   `javax.naming.directory.InitialDirContext.getAttributes`
-   `javax.naming.directory.InitialDirContext.getSchema`
-   `javax.naming.directory.InitialDirContext.getSchemaClassDefinition`
-   `javax.naming.directory.InitialDirContext.modifyAttributes`
-   `javax.naming.directory.InitialDirContext.rebind`
-   `javax.naming.directory.InitialDirContext.search`
-   `javax.naming.InitialContext.doLookup`
-   `javax.naming.InitialContext.lookup`
-   `javax.naming.spi.ContinuationContext.lookup`
-   `javax.naming.spi.ContinuationDirContext.bind`
-   `javax.naming.spi.ContinuationDirContext.createSubcontext`
-   `javax.naming.spi.ContinuationDirContext.getAttributes`
-   `javax.naming.spi.ContinuationDirContext.getSchema`
-   `javax.naming.spi.ContinuationDirContext.getSchemaClassDefinition`
-   `javax.naming.spi.ContinuationDirContext.getTargetContext`
-   `javax.naming.spi.ContinuationDirContext.modifyAttributes`
-   `javax.naming.spi.ContinuationDirContext.rebind`
-   `javax.naming.spi.ContinuationDirContext.search`
-   `javax.sql.rowset.spi.ProviderImpl.getDataSourceLock`
-   `javax.sql.rowset.spi.ProviderImpl.getProviderGrade`
-   `javax.sql.rowset.spi.ProviderImpl.getRowSetReader`
-   `javax.sql.rowset.spi.ProviderImpl.getRowSetWriter`
-   `javax.sql.rowset.spi.ProviderImpl.setDataSourceLock`
-   `javax.sql.rowset.spi.ProviderImpl.supportsUpdatableView`
-   `javax.sql.rowset.spi.SyncFactory.enumerateBindings`
-   `javax.sql.rowset.spi.SyncFactory.getInstance`
-   `javax.sql.rowset.spi.SyncFactory.initJNDIContext`
-   `javax.sql.rowset.spi.SyncFactory.parseJNDIContext`

A small subset of these are banned directly. The rest are banned indirectly by
banning the `lookup()`, `bind()`, `rebind()`, `getAttributes()`,
`modifyAttriutes()`, `createSubcontext()`, `getSchema()`,
`getSchemaClassDefinition()` and `search()` methods on any subclass
(implementer) of `javax.naming.Context`. The indirect ban is necessary due to
these methods being vulnerable in previously noted subclasses in the JDK. If
they were not banned at the Context level, a cast to Context would make the
vulnerable call invisible to static analysis.
