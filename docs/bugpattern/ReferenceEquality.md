Reference types that declare an `equals()` method, or that inherit `equals()`
from a type other than `Object`, should not be compared for reference equality
using `==` or `!=`. Instead, compare for value equality using `.equals()`.
