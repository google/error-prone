You should almost never invoke the `Enum.ordinal()` method. The ordinal exists
only to support low-level utilities like `EnumSet`. The ordinal of a given enum
value is not guaranteed to be stable across builds because of the potential for
enum values to be added, removed, or reordered.

Prefer using enum value directly:

```java
ImmutableMap<MyEnum, String> MAPPING =
    ImmutableMap.<MyEnum, String>builder()
        .put(MyEnum.FOO, "Foo")
        .put(MyEnum.BAR, "Bar")
        .buildOrThrow();
```

to this:

```java
ImmutableMap<Integer, String> MAPPING =
    ImmutableMap.<Integer, String>builder()
        .put(MyEnum.FOO.ordinal(), "Foo")
        .put(MyEnum.BAR.ordinal(), "Bar")
        .buildOrThrow();
```

Or if you need a stable number for serialisation, consider defining an explicit
field on the enum instead:

```java
enum MyStableEnum {
  FOO(1),
  BAR(2),
  ;

  private final int index;
  MyStableEnum(int index) {
    this.index = index;
  }
}
```
