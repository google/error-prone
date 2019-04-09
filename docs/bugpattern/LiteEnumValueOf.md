Byte code optimizers can change the implementation of `toString()` in lite
runtime and thus using `valueOf(String)` is discouraged. Instead of converting
enums to string and back, its numeric value should be used instead as it is the
stable part of the protocol defined by the enum.
