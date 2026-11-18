JUnit4 provides two annotations ([`@BeforeClass`][beforeclass] and
[`@AfterClass`][afterclass]) that are applied to methods that are run once per
**test class**. These complement the more-often used `@Before` and `@After`
which are applied to methods that are run once per **test method**.

JUnit4 runs `@BeforeClass` and `@AfterClass` methods without making an instance
of the test class, meaning that the methods must be `static`. JUnit4 will fail
to run any `@BeforeClass` or `@AfterClass` method that isn't also `static`.

[beforeclass]: https://junit.org/junit4/javadoc/latest/org/junit/BeforeClass.html
[afterclass]: https://junit.org/junit4/javadoc/latest/org/junit/AfterClass.html
