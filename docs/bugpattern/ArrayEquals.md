Generally when comparing arrays for equality, the programmer intends to check
that the the contents of the arrays are equal rather than that they are actually
the same object. But many commonly used equals methods compare arrays for
reference equality rather than content equality. These include the instance
.equals() method, Guava's com.google.common.base.Objects#equal(), JDK's
java.util.Objects#equals(), and Android's
android.support.v4.util.ObjectsCompat#equals.

If reference equality is needed, == should be used instead for clarity.
Otherwise, use java.util.Arrays#equals() to compare the contents of the arrays.
