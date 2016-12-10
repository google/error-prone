When a `Serializable` object is placed in an `android.os.Bundle`, certain types
whose serialization is handled with custom logic are 'flattened' to their base
types. Casting items retrieved from a bundle with `bundle.getSerializable(...)`
back to their original types may therefore cause a `ClassCastException`.

See [discussion here](https://code.google.com/p/android/issues/detail?id=3847).

Casting the result of `bundle.getSerializable(...)` to any subtype of any of the
following classes is forbidden[^1]:

*   `Map` (except `HashMap`)
*   `List` (except `ArrayList` or any `Parcelable`)
*   `SparseArray` (except `Parcelable`s)
*   `CharSequence` (except `String` or any `Parcelable`)
*   `CharSequence[]` (except `String[]`)

Note that `getSerializable` happens to return `HashMap`s for any `Map`
implementation or `ArrayList`s for any `List` implementation, however, this is
not guaranteed by the API. Therefore, casting to `ArrayList` or `HashMap` is
allowed, but it is safer to cast to the base `List` or `Map`.

An exception is made for most types if they also implement `Parcelable`, as they
are handled such that their type is preserved.

In many cases, you may be able to cast your object to its base type without any
ill effects if you don't require any features of a specific implementation.

In other cases, there may be an constructor that will create an instance of your
type from an instance of the base type (but beware of null results causing
IllegalArgumentExceptions!) For example:

```java
TreeMap<K, V> myMap;
Map<K, V> deserialized = (Map<K, V>) myBundle.getSerializable("KEY");
myMap = deserialized == null ? null : new TreeMap<K, V>(deserialized);
```

If you really need a different workaround, wrapping your object (e.g. of type
`MyList`) in an arbitrary wrapper class (e.g. `MyListHolder`) that implements
just `Serializable` will serialize and deserialize it through the standard
`Serializable` path and preserve your original type.

[^1]: Here is a full list of types that are handled specially in the
    bundling/parceling serialization/deserialization process, in order of
    precedence. If a serialized object inherits from multiple types on this
    list, it will be bundled as the first of those types to appear. e.g. if an
    object implements both Parcelable and List, it will be serialized as a
    Parcelable rather than a List, and its type will be preserved. Note also
    that it is okay to cast to `String` even though it inherits from
    `CharSequence` since `String` is handled first.

    *   `String`
    *   `Integer`
    *   `Map`
    *   `Bundle`
    *   `PersistableBundle`
    *   `Parcelable` - Parcelables are handled such that their type is
        preserved.
    *   `Short`
    *   `Long`
    *   `Float`
    *   `Double`
    *   `Boolean`
    *   `CharSequence`
    *   `List`
    *   `SparseArray`
    *   `boolean[]`
    *   `byte[]`
    *   `String[]`
    *   `CharSequence[]`
    *   `IBinder`
    *   `Parcelable[]`
    *   `int[]`
    *   `long[]`
    *   `Byte`
    *   `Size`
    *   `SizeF`
    *   `double[]`
    *   `Object[]` - Only handles pure Object[], not subtypes.
    *   All other `Serializable` objects go through the normal serialization
        path, types are preserved.
