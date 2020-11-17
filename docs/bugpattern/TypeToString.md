`Type#toString` shouldn't be used for Type comparison as it is expensive and
fragile.

If this code is within an Error Prone check for comparing `Type`(s), there are
better alternatives available.

Instead of

```java
type.toString().equals("com.package.SomeObject")
```

use

```java
visitorState.getTypes().isSameType(type, visitorState.getTypeFromString("com.package.SomeObject"))
```

For primitive types,

```java
type.getKind().equals(TypeKind.INT)
```

For object type, `java {.good}
type.getKind().equals(state.getSymtab().objectType)`
