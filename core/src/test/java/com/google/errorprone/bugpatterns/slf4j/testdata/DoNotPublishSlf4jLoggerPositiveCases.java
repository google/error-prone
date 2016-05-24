package com.google.errorprone.bugpatterns.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DoNotPublishSlf4jLoggerPositiveCases {
    // BUG: Diagnostic contains: Do not publish Logger field, it should be private
    public Logger publicLogger = LoggerFactory.getLogger(DoNotPublishSlf4jLoggerPositiveCases.class);

    // BUG: Diagnostic contains: Do not publish Logger field, it should be private
    protected org.slf4j.Logger protectedLogger;

    // BUG: Diagnostic contains: Do not publish Logger field, it should be private
    static Logger staticPackagePrivateLogger;
}
