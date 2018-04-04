Invoking a collection method with the same collection as the argument is likely
incorrect.

*   `collection.addAll(collection)` may cause an infinite loop, duplicate the
    elements, or do nothing, depending on the type of Collection and
    implementation class.
*   `collection.retainAll(collection)` is a no-op.
*   `collection.removeAll(collection)` is the same as `collection.clear()`.
*   `collection.containsAll(collection)` is always true.
