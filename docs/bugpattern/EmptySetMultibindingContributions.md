When using [Dagger Multibinding][dmb], you can use methods like the below to
make sure that there's a (potentially empty) Set binding for your type:

```java
@Provides @ElementsIntoSet Set<MyType> provideEmptySetOfMyType() {
  return new HashSet<>();
}
```

However, there's a slightly easier way to express this:

```java
@Multibinds abstract Set<MyType> provideEmptySetOfMyType();
```

[dmb]: https://google.github.io/dagger/multibindings.html
