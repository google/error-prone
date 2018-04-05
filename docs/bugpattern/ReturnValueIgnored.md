Certain library methods do nothing useful if their return value is ignored. For
example, String.trim() has no side effects, and you must store the return value
of String.intern() to access the interned string. This check encodes a list of
methods in the JDK whose return value must be used and issues an error if they
are not.
