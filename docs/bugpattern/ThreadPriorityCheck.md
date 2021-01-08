Don't rely on the thread scheduler for correctness or performance. Instead,
ensure that the average number of runnable threads is not significantly greater
than the number of processors, i.e. by using the executor framework and an
appropriately sized thread pool.

For more information, see [Effective Java 3rd Edition §84][ej3e-84].

[ej3e-84]: https://books.google.com/books?id=BIpDDwAAQBAJ
