The generated Java source files for Protocol Buffer enums have `getNumber()` as
accessors for the tag number in the protobuf file.

In addition, since it's a java enum, it also has the `ordinal()` method,
returning its positional index within the generated java enum.

The `ordinal()` order of the generated Java enums isn't guaranteed, and can
change when a new enum value is inserted into a proto enum. The `getNumber()`
value won't change for an enum value (since making that change is a
backwards-incompatible change for the protocol buffer).

You should very likely use `getNumber()` in preference to `ordinal()` in all
circumstances since it's a more stable value.

Note: If you're changing code that was already using ordinal(), it's likely that
getNumber() will return a different real value. Tread carefully to avoid
mismatches if the ordinal was persisted elsewhere.
