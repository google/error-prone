The following methods are not available in JDK 11:

*   `SecurityManager.checkTopLevelWindow`,
    `SecurityManager.checkSystemClipboardAccess`,
    `SecurityManager.checkAwtEventQueueAccess`,
    `SecurityManager.checkMemberAccess`
    (https://bugs.openjdk.java.net/browse/JDK-8193032)

*   `Runtime.runFinalizersOnExit`, `System.runFinalizersOnExit`
    (https://bugs.openjdk.java.net/browse/JDK-8198250)

*   `Thread.destroy`, `Thread.stop(Throwable)`
    (https://bugs.openjdk.java.net/browse/JDK-8204243)
