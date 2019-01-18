It is dangerous for a method to return a mutable instance in some circumstances,
but immutable in others. Doing so may lead users of your API to make incorrect
assumptions about the mutability of the return type. For example, consider this
method:

```java {.bad}
List<Integer> primeFactors(int n) {
  if (isPrime(n)) {
    return Collections.singletonList(n);
  }
  List<Integer> factors = new ArrayList<>();
  for (...) {
    factors.add(i);
  }
  return factors;
}
```

If someone were to add another method to include the trivial factor `1`, a bug
will be introduced.

```java
List<Integer> primeFactorsAndOne(int n) {
  List<Integer> primeFactors = primeFactors(n);
  primeFactors.add(1);
  return primeFactors;
}
```

`primeFactorsAndOne` will behave as intended for composite numbers, but throw an
exception for primes.
