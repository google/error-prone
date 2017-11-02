JUnit4 provides two annotations ([`@BeforeClass`][beforeclass] and
[`@AfterClass`][afterclass]) that are applied to methods that are run once per
**test class**. These complement the more-often used `@Before` and `@After`
which are applied to methods that are run one per **test class**.

JUnit4 runs `@BeforeClass` and `@AfterClass` methods without making an instance
of the test class, meaning that the methods must be `static`. JUnit4 will fail
to run any `@BeforeClass` or `@AfterClass` method that isn't also `static`.

[beforeclass]: http://junit.sourceforge.net/javadoc/org/junit/BeforeClass.html
[afterclass]: http://junit.sourceforge.net/javadoc/org/junit/AfterClass.html
