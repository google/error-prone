---
title: LiteProtoToString
summary: toString() on lite protos will not generate a useful representation of the proto from optimized builds. Consider whether using some subset of fields instead would provide useful information.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LiteProtoToString")` to the enclosing element.
