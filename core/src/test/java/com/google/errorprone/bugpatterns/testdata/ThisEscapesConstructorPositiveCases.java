package com.google.errorprone.bugpatterns.testdata;

public class ThisEscapesConstructorPositiveCases {

    static class AssignsThis {
        public AssignsThis at;
        AssignsThis() {
            // BUG: Diagnostic contains:
            at = this;
        }
    }

    static class PassesThis {
        PassesThis() {
            // BUG: Diagnostic contains:
            receivesThis(this);
        }

        private void receivesThis(PassesThis t) {}
    }
}
