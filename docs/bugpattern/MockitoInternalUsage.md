Classes under `org.mockito.internal.*` are internal implementation details and
are not part of Mockito's public API. Mockito team does not support them, and
they may change at any time. Depending on them may break your code when you
upgrade to new versions of Mockito.

This checker ensures that your code will not break with future Mockito upgrades.
Mockito's public API is documented at
https://www.javadoc.io/doc/org.mockito/mockito-core/. If you believe that there
is no replacement available in the public API for your use-case, contact the
Mockito team at https://github.com/mockito/mockito/issues.
