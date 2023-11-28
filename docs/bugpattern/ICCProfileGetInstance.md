`ICC_Profile.getInstance(String)` searches the entire classpath, which is often
unnecessary and can result in slow performance for applications with long
classpaths. Prefer `getInstance(byte[])` or `getInstance(InputStream)` instead.

See also https://bugs.openjdk.org/browse/JDK-8191622.
