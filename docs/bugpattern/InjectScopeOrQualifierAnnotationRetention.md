Qualifier and Scope annotations are used by dependency injection frameworks to adjust their
behavior. Not having runtime retention on scoping or qualifier annotations will cause unexpected
behavior in frameworks that use reflection:

```java
class CreditCardProcessor { @Inject CreditCardProcessor(...) }

@Qualifier
@interface ForTests

@Provides
@ForTests
CreditCardProcessor providesTestProcessor() { return new TestCreditCardProcessor(...) }
...

@Inject
MyApp(CreditCardProcessor processor) {
  processor.issueCharge(...); // Issues a charge against a fake!
}
```

Since the Qualifier doesn't have runtime retention, the Guice provider method doesn't see the
annotation, and will use the TestCreditCardProcessor for the normal CreditCardProcessor injection
point.

NOTE: While dependency injection frameworks that don't use this information at runtime (e.g. Dagger)
don't technically need runtime retention, the JSR-330 specification still requires runtime retention
for both [`Qualifier`] and [`Scope`]. This Error Prone check will attempt to conservatively detect
when a qualifier is very likely only used in Dagger (e.g.: it's nested inside a Dagger component),
but it will alert in all other cases.

[`Qualifier`]: http://docs.oracle.com/javaee/6/api/javax/inject/Qualifier.html
[`Scope`]: http://docs.oracle.com/javaee/6/api/javax/inject/Scope.html
