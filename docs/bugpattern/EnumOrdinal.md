You should almost never invoke the `Enum.ordinal()` method, nor depend on some
enum constant being at a particular index of the `values()` array. The ordinal
of a given enum value is not guaranteed to be stable across builds because of
the potential for enum values to be added, removed, or reordered. The ordinal
exists only to support low-level utilities like `EnumSet`.

Prefer using enum value directly:

```java
ImmutableMap<MyEnum, String> MAPPING =
    ImmutableMap.<MyEnum, String>builder()
        .put(MyEnum.FOO, "Foo")
        .put(MyEnum.BAR, "Bar")
        .buildOrThrow();
```

instead of relying on the ordinal:

```java
ImmutableMap<Integer, String> MAPPING =
    ImmutableMap.<Integer, String>builder()
        .put(MyEnum.FOO.ordinal(), "Foo")
        .put(MyEnum.BAR.ordinal(), "Bar")
        .buildOrThrow();
```

If you need a stable number for serialisation, consider defining an explicit
field on the enum:

```java
enum MyStableEnum {
  FOO(1),
  BAR(2),
  ;

  private final int wireCode;
  MyStableEnum(int wireCode) {
    this.wireCode = wireCode;
  }
}
```

rather than relying on the ordinal values:

```java
enum MyUnstableEnum {
  FOO,
  BAR,
}
MyUnstableEnum fromWire(int wireCode) {
  return MyUnstableEnum.values()[wireCode];
}
```
