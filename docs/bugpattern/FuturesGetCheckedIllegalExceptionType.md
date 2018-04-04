The passed exception type must not be a RuntimeException, and it must expose a
public constructor whose only parameters are of type String or Throwable.
getChecked will reject any other type with an IllegalArgumentException.
