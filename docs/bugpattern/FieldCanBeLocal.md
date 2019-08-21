A field which is always assigned before being used from every method that reads
it may be better expressed as a local variable. Using a field in such cases
gives the impression of mutable state in the class where it isn't necessary.

```java
class Frobnicator {
  private int sum = 0;

  int sum(int a, int b) {
    sum = a + b;
    return sum;
  }

  int sum(int a, int b, int c) {
    sum = a + b + c;
    return sum;
  }
```

```java
class Frobnicator {
  int sum(int a, int b) {
    int sum = a + b;
    return sum;
  }

  int sum(int a, int b, int c) {
    int sum = a + b + c;
    return sum;
  }
```

## Suppression

This check is suppressed by `@SuppressWarnings("FieldCanBeLocal")` as well as
the same suppression mechanisms as `UnusedVariable`
(`@SuppressWarnings("unused")`.

