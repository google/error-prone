`Hashtable.contains(Object)` and `ConcurrentHashMap.contains(Object)` are
legacy methods for testing if the given object is a value in the hash table. 
They are often mistaken for `containsKey`, which checks whether the given object
is a *key* in the  hash table.

If you intended to check whether the given object is a key in the hash table,
use `containsKey` instead.  If you really intended to check whether the 
given object is a value in the hash table, use `containsValue` for clarity.
