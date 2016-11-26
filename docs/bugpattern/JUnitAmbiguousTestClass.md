For JUnit3-style tests, behavior is defined in `junit.framework.TestCase` and
tests add behavior by overriding methods. For JUnit4-style tests, special
behavior happens with fields and methods annotated with JUnit 4 annotations.
Having JUnit4-style tests extend from `junit.framework.TestCase` (directly or
indirectly) historically has been a source of test bugs and unexpected behavior
(e.g.: teardown logic and/or verification does not run because JUnit doesn't
call the inherited code).

Error Prone also cannot infer whether the test class runs with JUnit 3 or JUnit
4.

Thus, even if the test class runs with JUnit 4, Error Prone will not run
additional checks which can catch common errors with JUnit 4 test classes.
Either use only JUnit4 classes and annotations and remove the inheritance from
TestCase, or use only JUnit 3 and remove the `@Test` annotations. When looking
for replacements for base test classes, consider using Rules (see the `@Rule`
annotation and implementations of `TestRule` and `MethodRule`).
