As per JLS 15.21.1, == NaN comparisons always return false, even NaN == NaN.
Instead, use the isNaN methods to check for NaN.
