> When passing a mutable instance into a method which makes a deep copy of the
> instance, it results in discrepancies between the forked instances. It is
> prone to errors if one expects the state change on the original instance to be
> reflected on the method with its forked instance.


```java {.bad}
Location location = new Location("gps");
Bundle bundle = new Bundle();
bundle.putFloat("someFloat", 12.3f);
location.setExtras(bundle);

// Now add more things to the bundle, but it won't modify the internal
// representation stored by Location.
bundle.putInt("someInt", 7);
```

```java {.bad}
private static Bundle getLocationExtras(Location location) {
  Bundle bundle = location.getExtras();
  if (bundle != null) {
    return bundle;
  }
  bundle = new Bundle();
  location.setExtras(bundle);

  // Now leaks the bundle which is subject to modification in its
  // method of invocation.
  return bundle;
}
```
