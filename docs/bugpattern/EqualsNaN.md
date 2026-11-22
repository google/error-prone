As per the Java Language Specification, == comparisions with NaN always return false, including NaN == NaN.
Instead, use the isNaN methods to test for NaN values.