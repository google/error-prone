Qualifiers and Scoping annotations have different semantic meanings and a single
annotation should not be both a qualifier and a scoping annotation.

If an annotation is both a scoping annotation and a qualifier, unless great care
is taken with its application and usage, the semantics of objects annotated with
the annotation are unclear.

Take a look at this example:

```java
@Retention(RetentionPolicy.RUNTIME)
@Scope
@Qualifier
@interface DayScoped {}

static class Allowance {}
static class DailyAllowance extends Allowance {}
static class Spender {
  @Inject
  Spender(Allowance allowance) {}
}

static class BindingModule extends AbstractModule {
  ...
  @Provides
  @DayScoped
  Allowance providesAllowance() {
    return new DailyAllowance();
  }
}
```

Here, the `Allowance` instance used by Spender isn't actually scoped to a single
day, as the `@Provides` method applies the `DayScoped` scoping only to the
`@DayScoped Allowance`. Instead, the default constructor of `Allowance` is used
to create a new instance every time a `Spender` is created.

If `@DayScope` wasn't a `Qualifier`, the provider method would do the
right thing: the un-annotated `Announce` binding would be scoped to DayScope,
implemented by a single `DailyAllowance` instance per day.
