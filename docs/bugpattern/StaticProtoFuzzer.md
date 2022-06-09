ProtoFuzzer is a mutable class, even when seeded by a compile-time constant.
We're trying to avoid the following pitfalls which can arise when assigning a
ProtoFuzzer to a static field:

*   Accidental state leakage between unit tests, causing non-deterministic or
    flaky behavior. In this scenario, it's recommended to instantiate a distinct
    ProtoFuzzer for each relevant unit test, possibly by using a `@Before`
    method.

``` {.good}
 private ProtoFuzzer protoFuzzer;
 ...
  @Before
  public void setUp() {
    ...
    // Customize as appropriate
    protoFuzzer = ProtoFuzzer.newBuilder().setSeed(...).build();
    ...
  }
```

*   If a static ProtoFuzzer is used to initialize other static fields, then this
    initialization process can have program-order dependency; for example,
    re-ordering two such initialized fields can cause their values to change.
    This problem can be avoided by using static builder methods to initialize.

``` {.bad}
private static final ProtoFuzzer protoFuzzer =
    ProtoFuzzer.newBuilder()
               .setSeed(...)
               .build();
...
// Re-ordering myFirstCustomProto and mySecondCustomProto can change their values!
private static final MyCustomProto myFirstCustomProto =
    protoFuzzer.makeMessageOfType(
      MyCustomProto.getDefaultInstance()
    );
private static final MyCustomProto mySecondCustomProto =
    protoFuzzer.makeMessageOfType(
      MyCustomProto.getDefaultInstance()
    );

```

Instead, create a static builder method and replace references to the static
ProtoFuzzer field with calls to the builder:

``` {.good}

private static final MyCustomProto myFirstCustomProto =
  buildMyCustomProtoFuzzer().makeMessageOfType(
    MyCustomProto.getDefaultInstance()
  );
private static final MyCustomProto mySecondCustomProto =
  buildMyCustomProtoFuzzer().makeMessageOfType(
    MyCustomProto.getDefaultInstance()
  );
...
private static ProtoFuzzer buildMyCustomProtoFuzzer() {
  // Customize as appropriate
  return ProtoFuzzer.newBuilder().setSeed(...).build();
}
```
