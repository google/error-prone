The contract for `Object.hashCode` states that if two objects are equal, then
calling the `hashCode()` method on each of the two objects must produce the same
result. Implementing `equals()` but not `hashCode()` causes broken behaviour
when trying to store the object in a collection.

See [Effective Java 3rd Edition §11][ej3e-11] for more information and a
discussion of how to correctly implement `hashCode()`.

[ej3e-11]: https://books.google.com/books?id=BIpDDwAAQBAJ
