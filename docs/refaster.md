---
title: Refaster templates
layout: documentation
---

In addition to [patching] your code using the checks built-in to Error Prone, we've developed a mechanism to refactor your code using before-and-after templates (we call them Refaster templates). Once you write these templates, you compile them into .refaster files, then use the Error Prone compiler to refactor your code according to those rules.

Refaster is described in more detail in a [research paper][lowasser-paper] presented by Louis Wasserman at the _Workshop for Refactoring Tools_.

## Building Refaster Templates

Explaining how to write refaster rules is best done by example:

```java
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

public class StringIsEmpty {
  @BeforeTemplate
  boolean equalsEmptyString(String string) {
    return string.equals("");
  }

  @BeforeTemplate
  boolean lengthEquals0(String string) {
    return string.length() == 0;
  }

  @AfterTemplate
  @AlsoNegation
  boolean optimizedMethod(String string) {
    return string.isEmpty();
  }
}
```

Refaster templates are any class with multiple methods with the same return type and list of arguments with the same name. One of the methods should be annotated `@AfterTemplate`, and every other method should be annotated with `@BeforeTemplate`. With this template, any code calling `String#equals` passing in the empty string literal, or calling `String#length` and comparing it to 0 will be replaced by a call to `String#isEmpty`. Notably, no matter how the String expression is generated, Refaster will do the replacement:

```java
boolean b = someChained().methodCall().returningAString().length() == 0;
```

becomes

```java
boolean b = someChained().methodCall().returningAString().isEmpty();
```

while

```java
if (this.someStringField.equals(""))
```

becomes

```java 
if (this.someStringField.isEmpty())
```

There are other annotations in the [refaster.annotations] package that allow you to express more complex before-and-after refactorings, with examples about how to use them in their javadoc. See also the [advanced features](#advanced-features) section of this document.

In the above example, `@AlsoNegation` is used to signal that the rule can also match the logical negation of the `@BeforeTemplate` bodies (`string.length() != 0` becomes `!string.isEmpty()`);

## Running the Refaster refactoring

TIP: These instructions are valid as of the most recent snapshot at HEAD, and are subject to change

Use the error prone javac jar and the error_prone_refaster jar to compile the refaster template:

```shell
wget http://repo1.maven.org/maven2/com/google/errorprone/javac/9-dev-r3297-4/javac-9-dev-r3297-4.jar
wget http://repo1.maven.org/maven2/com/google/errorprone/error_prone_refaster/2.0.18/error_prone_refaster-2.0.18.jar

java -cp javac-9-dev-r3297-4.jar:error_prone_refaster-2.0.18.jar \
  com.google.errorprone.refaster.RefasterRuleCompiler \
  StringIsEmpty.java --out `pwd`/myrule.refaster
 ```

 You should see a file named `myrule.refaster` in your current directory. To use this to refactor your code, add the following flags to the Error Prone compiler (this is similar to [patching]):

 ```
-XepPatchChecks:refaster:/full/path/to/myrule.refaster
-XepPatchLocation:/full/path/to/your/source/root
```

This will generate a unified diff file named `error-prone.patch` that you can apply similarly to how you would apply other patches:

```shell
cd /full/path/to/your/source/root
patch -p0 -u -i error-prone.patch
```

## Anatomy of a Refaster Rule

The idea of Refaster is, more or less, to take the pseudocode you'd write to explain a refactoring you wanted to perform, and make that into real code that performs a real refactoring.

Let's take apart an example rule.

```java
class Utf8Length { // A name for the refactoring
  @BeforeTemplate // This is what the code looks like before the refactoring
  int toUtf8Length( // the method name is unimportant
      String string /* the string parameter stands in for any expression of type String */) {
    return /* this is here just to make the compiler happy */
        string.getBytes(StandardCharsets.UTF_8).length; // this is what the code looks like before the refactoring
  }

  @AfterTemplate // replace code with this pattern
  int optimizedMethod(
      String string /* substitute in the original String expression */) {
    return Utf8.encodedLength(string);
  }
}
```

This refactoring rewrites _expressions_ of the form `someString.getBytes(UTF_8).length` to `Utf8.encodedLength(string)`, a method from Guava that avoids allocating an entire byte array just to get its length, no matter where that `String` comes from -- it can be the result of a more complicated expression, or just a simple variable.  Additionally, this will match even if the `int` result of the expression is being passed to a method, added to something else, assigned to a variable, whatever.

This is called an _expression_ template because it rewrites expressions.  Let's look at a _block_ template:

```java
class ListSwap<T> { // T is a type parameter for the entire rule, which will be inferred from the match
  @BeforeTemplate
  void manualSwap(List<T> list, int i, int j) { // the list, i, and j can be any expressions of appropriate type
    T tmp = list.get(i); // tmp doesn't actually have to be the variable name used in the code being matched
    list.set(i, list.get(j));
    list.set(j, tmp);
  }
  @AfterTemplate
  void swap(List<T> list, int i, int j) {
    Collections.swap(list, i, j);
  }
}
```

This rule matches three consecutive lines that "look like" the body of the `@BeforeTemplate`, and replaces them with the single line of the `@AfterTemplate`.

## What is Refaster good for?

Refaster was originally built for the Java Libraries Team at Google, around the slogan of "make simple refactorings simple."  We tended to focus on library migrations, method renamings, and the like; refactorings that could be clearly described by just describing what the code should look like before and after the change.  We use Refaster for simple refactorings like this, and combine it with more sophisticated analyses written with the Error Prone API for refactorings Refaster can't express.

Refaster excels at refactorings like:

 * Migrate users of method A to method B.
 * Migrate users of method A where the arguments are of some particular type to method B.
 * Migrate a particular fluent sequence of method invocations to some other pattern.
 * Migrate a sequence of consecutive statements to an alternative.

We have also found Refaster good for expressing code _simplification_ patterns to improve efficiency or code readability.  For example, here is a simplification rule for Java 8 streams that Refaster expresses neatly:

```java
class SortedFirst<T> {
  @BeforeTemplate
  Optional<T> before(Stream<T> stream, Comparator<? super T> comparator) {
    return stream.sorted(comparator).findFirst();
  }
  @AfterTemplate
  Optional<T> after(Stream<T> stream, Comparator<? super T> comparator) {
    return stream.min(comparator);
  }
}
```

We have literally thousands of simplification patterns like this that are presented as automatically generated code review comments at Google that we hope to release in open source soon.

## Advanced features

### `Refaster.anyOf`

A particularly commonly used method is `Refaster.anyOf`, a "magic" method for
use in your `@BeforeTemplate` which indicates that any of the specified
expressions are allowed to match. For example:

```java
class AddAllArrayToBuilder<E> {
  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllAsList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Refaster.anyOf(
        Arrays.asList(elements),
        ImmutableList.copyOf(elements),
        Lists.newArrayList(elements)));
  }

  @AfterTemplate
  ImmutableCollection.Builder<E> addAll(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    builder.add(elements);
  }
}
```

is equivalent to, but much shorter than,

```java
class AddAllArrayToBuilder {
  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllArraysAsList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Arrays.asList(elements));
  }

  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllImmutableListCopyOf(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(ImmutableList.copyOf(elements));
  }

  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllNewArrayList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Lists.newArrayList(elements));
  }

  @AfterTemplate
  ImmutableCollection.Builder<E> addAll(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    builder.add(elements);
  }
}
```

### `Refaster.clazz()` and other methods

Consider the following refactoring, where calling X.class.cast(o) for any X is replaced with a simple cast to X.

```java
class ClassCast<T> {
  @BeforeTemplate
  T cast(Object o) {
    return Refaster.<T>clazz().cast(o);
  }

  @AfterTemplate
  T cast(Object o) {
    return (T) o;
  }
}
```

Here, `Refaster.<T>clazz()` is a "magic incantation" to substitute for the
impossible-to-compile code you would _want_ to write here, `T.class`. There are
a variety of these "magic incantations" in the [`Refaster` class][refaster-javadoc]
for code patterns that you might wish to write in a `@BeforeTemplate` but don't
technically compile as Java code.

### `@UseImportPolicy`

Refaster attempts to automatically infer how to import newly used classes and methods into refactored code, but the approach it uses to do so can be configured with the `@UseImportPolicy` annotation on the `@AfterTemplate` method.  (Note that Refaster will _not_ pay attention to how the class is imported or qualified in the original Refaster template!)  The `ImportPolicy` enum offers the following options:

 * `IMPORT_TOP_LEVEL`, the default: imports the top-level class and explicitly qualifies references to nested classes.  For example, to refer to `java.util.Map.Entry`, Refaster will `import java.util.Map;` and refer to the type as `Map.Entry`.
 * `IMPORT_CLASS_DIRECTLY` imports classes directly, whether nested in other classes or not: e.g. `import java.util.Map.Entry;`
 * `STATIC_IMPORT_ALWAYS` static imports methods explicitly mentioned in the Refaster template, and refers to types themselves as in `IMPORT_TOP_LEVEL`.

### Placeholder methods

#### Expression placeholders

Placeholder methods are a particularly powerful feature of Refaster, allowing
you to match arbitrary chunks of code in terms of arguments, not just
expressions of a given type. Here is a basic example usage of a placeholder:

```java
abstract class ComputeIfAbsent<K, V> {
  /*
   * Represents an arbitrary expression in terms of an input, key.
   */
  @Placeholder
  abstract V function(K key);

  @BeforeTemplate
  void before(Map<K, V> map, K key) {
    if (!map.containsKey(key)) {
      map.put(key, function(key));
    }
  }

  @AfterTemplate
  void after(Map<K, V> map, K key) {
    map.computeIfAbsent(key, (K k) -> function(k));
  }
}
```

We annotate an `abstract` method in the Refaster template class to represent
"some function in terms of the specified input." By default, arguments to the
placeholder _must_ be used for the placeholder to match. For example, this
pattern would _not_ match

```java
if (!map.containsKey(k)) {
  map.put(k, 0);
}
```

because the expression `0` does not refer to `k`. To change this behavior, you
may annotate the arguments to the placeholder method with `@MayOptionallyUse`:

```java
@Placeholder
abstract V function(@MayOptionallyUse K key);
```

Note also that the code matched by the placeholder method _cannot_ refer to
variables in the `@BeforeTemplate` that are not explicitly passed in. So

```java
if (!map.containsKey(k)) {
  map.put(k, map.get(k - 1));
}
```

would _not_ be matched, because the expression in the `put` refers to `map`,
which is not passed into the placeholder method. The match _can_ refer to
variables that aren't explicitly mentioned in the Refaster pattern, e.g.

```java
if (!map.containsKey(k)) {
  map.put(k, k + ":" + suffixString);
}
```

because `suffixString` is not a variable in the `@BeforeTemplate`.

##### Matching the identity

By default, placeholder methods are not permitted to simply pass on one of their
arguments unchanged. This behavior can be overridden with the annotation:
`@Placeholder(allowsIdentity = true)`.

#### Block placeholders

The above example used placeholders to match single expressions in terms of
other expressions, but not multiple lines of code. These, too, are supported.
Consider the following refactoring:

```java
abstract class IfSetAdd<E> {
  @Placeholder
  abstract void doAfterAdd(E element);

  @BeforeTemplate
  void ifNotContainsThenAdd(Set<E> set, E elem) {
    if (!set.contains(elem)) {
      set.add(elem);
      doAfterAdd(elem);
    }
  }

  @AfterTemplate
  void ifAdd(Set<E> set, E elem) {
    if (set.add(elem)) {
      doAfterAdd(elem);
    }
  }
}
```

...which would e.g. rewrite

```java
if (!mySet.contains(e)) {
  mySet.add(e);
  log("added %s to set", e);
}
```

to

```java
if (mySet.add(e)) {
  log("added %s to set", e);
}
```

There is also some limited magic supported here with block versus expression
lambdas. For example, consider the refactoring

```java
abstract class MapEntryLoop<K, V> {
  @Placeholder
  abstract void doSomething(K k, V v);

  @BeforeTemplate
  void entrySetLoop(Map<K, V> map) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      doSomething(entry.getKey(), entry.getValue());
    }
  }

  @AfterTemplate
  void mapForEach(Map<K, V> map) {
    map.forEach((K key, V value) -> doSomething(key, value));
  }
}
```

would rewrite

```java
for (Map.Entry<String, Integer> e : map.entrySet()) {
  System.out.println(e.getKey() + ":" + e.getValue());
}
for (Map.Entry<String, Integer> e : map.entrySet()) {
  String str = e.getKey() + ":" + e.getValue();
  System.out.println(str);
}
```

to

```java
map.forEach(
    (String key, Integer value) ->
        System.out.println(key + ":" + value));
map.forEach(
    (String key, Integer value) -> { // multiple lines!
        String str = key + ":" + value;
        System.out.println(str);
    });
```

...that is, it will automatically bracket the lambda body, or not, as
appropriate to the placeholder body actually matched.


[patching]: patching
[refaster.annotations]: ../api/latest/com/google/errorprone/refaster/annotation/package-frame.html
[lowasser-paper]: https://research.google.com/pubs/archive/41876.pdf
[refaster-javadoc]: ../api/latest/com/google/errorprone/refaster/Refaster.html
