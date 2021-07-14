The [Google Java Style Guide §5.2][style] provides rules for naming nembers.

## Test methods

The style guide includes the following special case for test methods:

> Underscores may appear in JUnit test method names to separate logical
> components of the name, with each component written in `lowerCamelCase`, for
> example `transferMoney_deductsFromSource`.

Note that this only applies to test methods themselves, not to helper methods
called by test methods.

```java
@Test
public void transferMoney_deductsFromChecking() {
  transferMoney_deductsFromSource(Source.CHECKING);
}

@Test
public void transferMoney_deductsFromSavings() {
  transferMoney_deductsFromSource(Source.SAVINGS);
}

// this method name shouldn't use underscores
private void transferMoney_deductsFromSource(Source source) { ... }
```

Instead of having a group of test methods call the same helper method with
varying parameters, consider a [parameterized test][parameterized] , or
splitting the tests into separate setup and assert helper methods (named
appropriately) to make the flow of each test clearer.

[style]: https://google.github.io/styleguide/javaguide.html#s5.2-specific-identifier-names

[parameterized]: https://junit.org/junit4/javadoc/4.12/org/junit/runners/Parameterized.html
