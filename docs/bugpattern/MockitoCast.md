The JDK9 javac fixes a bug ([JDK-8058199]
(https://bugs.openjdk.java.net/browse/JDK-8058199)) that was causing checkcast
instructions to sometimes be skipped. Previously javac used the parameter types
of a method symbol's erased type as targets when translating the arguments. In
JDK 9, javac has been fixed to use the inferred types as targets. The fix causes
additional checkcasts to be generated if the inferred types do not have the same
erasure.

The fix breaks Mockito answer strategies that pick types based on the erased
method signature's return type, and causes tests to fail with
ClassCastExceptions when compiled with the JDK 9 javac.

This check is a work-around until the Mockito bug is fixed: [mockito#357]
(https://github.com/mockito/mockito/issues/357)

The affected answer strategies include:

*   [`RETURNS_DEEP_STUBS`]
    (http://site.mockito.org/mockito/docs/current/org/mockito/Mockito.html#RETURNS_DEEP_STUBS)
*   [`RETURNS_MOCKS`]
    (http://site.mockito.org/mockito/docs/current/org/mockito/Mockito.html#RETURNS_MOCKS)
*   [`RETURNS_SMART_NULLS`]
    (http://site.mockito.org/mockito/docs/current/org/mockito/Mockito.html#RETURNS_SMART_NULLS)

The [`RETURNS_DEFAULTS`]
(http://site.mockito.org/mockito/docs/current/org/mockito/Mockito.html#RETURNS_DEFAULTS)
strategy is usually unaffected, because it returns `null` as the default value
of methods that return `Object`, and unbounded type parameters erase to
`Object`.

## Example:

```java
class Foo {
  <T> T getFirst(Iterable<T> xs) {
    return xs.iterator().next();
  }
}
```

```java
class Test {
  @Mock Foo f;

  @Test
  public void test() {
    Iterable<Boolean> it = Arrays.asList(false);
    when(f.getFirst(it)).thenReturn(false);
  }
}
```

The JDK8 javac would have translated `when(f.getFirst(it))` as:

```
INVOKEVIRTUAL Foo.getFirst (Ljava/lang/Iterable;)Ljava/lang/Object;
INVOKESTATIC org/mockito/Mockito.when (Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing;
```

The JDK9 javac translates it as:

```
INVOKEVIRTUAL Foo.getFirst (Ljava/lang/Iterable;)Ljava/lang/Object;
CHECKCAST java/lang/Boolean
INVOKESTATIC org/mockito/Mockito.when (Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing;
```

The erased return type of `Foo.getFirst` is `Object`, but the inferred return
type of `getFirst(Iterable<Boolean>)` is `Boolean`. If the answer strategy
returns `Object` the checkcast fails.
