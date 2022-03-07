package com.google.errorprone.bugpatterns.testdata;

public class ThisEscapesConstructorPositiveCases {

    /**
     * "this" should never appear as the right hand side of an assignment in a
     * constructor.
     */
    static class AssignsThis {
        public AssignsThis at;
        AssignsThis() {
            // BUG: Diagnostic contains:
            at = this;
        }
    }

    /**
     * "this" should never be passed as an argument to a method in a constructor.
     */
    static class PassesThisToSelf {
        PassesThisToSelf() {
            // BUG: Diagnostic contains:
            receivesThis(this);
        }

        private void receivesThis(PassesThisToSelf t) {}
    }

    static class PassesThisToOther {
        public int x;
        PassesThisToOther() {
            x = 6;
            // BUG: Diagnostic contains:
            Other.someMethod(this);
        }
    }

    static class Other {
        public static void someMethod(PassesThisToOther p) {
            p.x++;
        }
    }
}
