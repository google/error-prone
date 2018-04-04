Methods with the @UnlockMethod annotation are expected to release one or more
locks. The caller must hold the locks when the function is entered, and will not
hold them when it completes.
