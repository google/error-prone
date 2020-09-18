Including a default case is redundant when switching on an enum type if the
switch handles all possible values of the enum, and execution cannot continue
below the switch from any of the non-default statement groups.

Note: This check does not apply to pseudo-enums such as Android `@IntDef`s,
which are integers that are treated specially by other tools.

TIP: Removing the unnecessary default allows Error Prone to enforce that the
switch continues to handle all cases, even if new values are added to the enum,
see: [MissingCasesInEnumSwitch](MissingCasesInEnumSwitch.md). After the
unnecessary default is removed, Error Prone will report an error if new enum
constants are added in the future, to remind you to either handle the cases
explicitly or restore the default case.

## When the default can be removed

This check does not report cases where execution can continue after the switch
statement from any non-default statement groups. For example, consider:

```java
enum TrafficLightColour { RED, GREEN, YELLOW }

void approachIntersection(TrafficLightColour state) {
  boolean stop;
  switch (state) {
    case GREEN:
      stop = false;
      break;
    case YELLOW:
    case RED:
      stop = true;
      break;
  }
  if (stop) {
    ...
  }
}
```

The definition of control flow in [JLS §14.21] does not consider whether enum
switches handle all cases, so in the example above javac will complain that
`stop` is not definitely assigned. This is because adding constants to an enum
is a binary compatible change (see [JLS §13.4.26]), so the spec allows for the
possibility that `TrafficLightColour` is defined in another library, and after
compiling our code we update to a new version of the library (without
recompiling) that adds another colour of traffic light (say, `PURPLE`) that the
switch doesn't handle.

This check should be used together with `MissingCasesInEnumSwitch` in
environments where that kind of binary incompatibility is very unlikely. For
example, if your build system accurately tracks changes to dependencies and you
are deploying an application (instead of a library), the risk of skew between
compile-time and runtime is minimal. One the other hand, if you are a library
author and your code switches on an enum in a different library, you want want
to include 'defensive' default cases to handle the situation where a user
deploys your code together with an incompatible version of the other library.

[JLS §14.21]: https://docs.oracle.com/javase/specs/jls/se10/html/jls-14.html#jls-14.21
[JLS §13.4.26]: https://docs.oracle.com/javase/specs/jls/se10/html/jls-13.html#jls-13.4.26

## Examples

The following examples show situations where it is usually safe to remove the
default from an exhaustive enum switch.

### All cases return or throw

Before:

```java
enum State { ON, OFF }

boolean isOn(State state) {
  switch (state) {
    case ON:
      return true;
    case OFF:
      return false;
    default:
      throw new AssertionError("unknown state: " + state);
  }
}
```

After:

```java
enum State { ON, OFF }

boolean isOn(State state) {
  switch (state) {
    case ON:
      return true;
    case OFF:
      return false;
  }
  throw new AssertionError("unknown state: " + state);
}
```

### The default case is empty

Before:

```java
enum State { ON, OFF }

boolean isOn(State state) {
  switch (state) {
    case ON:
      return true;
    case OFF:
      break;
    default:
      break;
  }
  return false;
}
```

After:

```java
enum State { ON, OFF }

boolean isOn(State state) {
  switch (state) {
    case ON:
      return true;
    case OFF:
      break;
  }
  return false;
}
```

## Cases with UNRECOGNIZED

When a switch statement handles all values of a proto-generated enum except for
UNRECOGNIZED, the UNRECOGNIZED case should be explicitly handled and the default
should be removed. This is preferred so that `MissingCasesInEnumSwitch` will
catch unexpected enum types at compile-time instead of runtime.

If the switch statement cannot [complete normally], the default should be
deleted and its statements moved after the switch statement. The UNRECOGNIZED
case should be added with a break.

If it can complete normally, the default should be merged with an added
UNRECOGNIZED case.

[complete normally]: https://docs.oracle.com/javase/specs/jls/se10/html/jls-14.html#jls-14.1
