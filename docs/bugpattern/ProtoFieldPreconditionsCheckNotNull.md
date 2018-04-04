This checker looks for comparisons of protocol buffer fields with null via the
com.google.common.base.Preconditions.checkNotNull method. If a proto field is
not specified, its field accessor will return a non-null default value. Thus,
the result of calling one of these accessors can never be null, and comparisons
like these often indicate a nearby error.

If you meant to check whether an optional field has been set, you should use the
hasField() method instead.
