After bytecode optimization, Lite protos do not generate a meaningful
`#toString()`. This can be acceptable for debugging, as development builds will
typically not be optimized, but can pose a problem if the default lite
`#toString` finds its way into production code as part of an important error
message.

```java {.bad}
  public void validate(MyProto myProto) {
    if (!myProto.hasFrobnicator()) {
      // MyProto missing frobnicator: asf@6531767b
      throw new IllegalArgumentException("MyProto missing frobnicator: " + myProto);
    }
  }
```

The fix will be highly dependent on the case in point. There may be an
identifier associated with the message that would be useful to log, or even some
serialized version of the entire proto.

NOTE: Logging fields of a proto will force those fields to be retained after
optimization. This is not an issue if they're already being used, but writing a
comprehensive `#toString` method for a proto which is otherwise largely unused
would increase code size.
