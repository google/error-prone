Prefer using the format

```java
  when(mock.mockedMethod(...)).thenReturn(returnValue);
```

to initialise mocks, rather than,

```java
  doReturn(returnValue).when(mock).mockedMethod(...);
```

Mockito recommends the `when`/`thenReturn` syntax as it is both more readable
and provides type-safety: the return type of the stubbed method is checked
against the stubbed value at compile time.

There are certain situations where `doReturn` is required:

*   Overriding previous stubbing where the method will *throw*, as `when` makes
    an actual method call.
*   Overriding a `spy` where the method call where calling the spied method
    brings undesired side-effects.
