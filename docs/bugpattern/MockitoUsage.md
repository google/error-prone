Calls to `Mockito.when` should always be accompanied by a call to a method
like `thenReturn`.

```java
when(mock.get()).thenReturn(answer); // correct
when(mock.get())                     // oops!
```

Similarly, calls to `Mockito.verify` should call the verified method *outside*
the call to `verify`.

```java
verify(mock).execute(); // correct
verify(mock.execute()); // oops!
```

For more information, see the [Mockito documentation][docs].

[docs]: http://github.com/mockito/mockito/wiki/FAQ#what-are-unfinished-verificationstubbing-errors
