package com.google.errorprone.bugpatterns.slf4j;

import java.util.logging.Logger;

class DoNotPublishSlf4jLoggerNegativeCases {
    @SuppressWarnings("unused")
    private static org.slf4j.Logger privateStaticLogger;

    @SuppressWarnings("unused")
    private org.slf4j.Logger privateLogger = null;

    public java.util.logging.Logger julLogger = Logger.getAnonymousLogger();
}
