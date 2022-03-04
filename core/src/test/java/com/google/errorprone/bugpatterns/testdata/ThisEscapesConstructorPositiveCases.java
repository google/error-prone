package com.google.errorprone.bugpatterns.testdata;

public class ThisEscapesConstructorPositiveCases {

    static class AssignsThis {
        public AssignsThis at;
        AssignsThis() {
            // BUG: Diagnostic contains: This escapes constructor
            at = this;
        }
    }

    static class PassesThis {
        PassesThis() {
            // BUG: Diagnostic contains: This escapes constructor
            receivesThis(this);
        }

        private void receivesThis(PassesThis t) {}
    }
}
