---
title: ListRemoveAmbiguous
summary: Ambiguous call to List.remove; clarify if index-based or value-based removal
  was intended by adding a comment
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
There are two overloads of the method `List.remove`:
[`remove(int index)`][index] removes an element at the specified index, and
[`remove(Object element)`][element] removes the specified element. When used
with a list of integers (`List<Integer>`), the overload resolution can be
confusing and may lead to bugs if the wrong overload is selected or if the
code's intent is not clear to readers.

[index]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#remove(int)
[element]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#remove(java.lang.Object)

Consider the following code:

```java
List<Integer> list = new ArrayList<>();
// ...
list.remove(1);
```

In this case, `list.remove(1)` calls `remove(int index)`, removing the element
at index 1. If the intention was to remove the element with value 1, this code
is incorrect.

To make the intent explicit and avoid ambiguity, add a comment before the
argument:

If you meant to remove by value (element):

```java
list.remove(/* element */ ii);
```

If you meant to remove by index:

```java
list.remove(/* index */ 1);
```

If you need to change the type to select the correct overload, you can use
explicit boxing/unboxing:

```java
list.remove(Integer.valueOf(i)); // remove by element
list.remove(ii.intValue());      // remove by index
```

[JDK-8384074](https://bugs.openjdk.org/browse/JDK-8384074) discusses adding a
new `List::removeAtIndex` method to the JDK to avoid this potential overload
confusion.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ListRemoveAmbiguous")` to the enclosing element.
