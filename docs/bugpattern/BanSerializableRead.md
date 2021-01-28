The Java `Serializable` API is very powerful, and very dangerous. Any
consumption of a serialized object that cannot be explicitly trusted will likely
result in a critical remote code execution bug that will give an attacker
control of the application. (See
[Effective Java 3rd Edition §85][ej3e-85])

[ej3e-85]: https://www.google.co.uk/books/edition/Effective_Java/ka2VUBqHiWkC

Consider using less powerful serialization methods, such as JSON or XML.
