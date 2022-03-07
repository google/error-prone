package com.google.errorprone.bugpatterns.testdata;

public class ThisEscapesConstructorNegativeCases {

    /**
     * Legal usage of "this" in the constructor is on the left hand side of
     * an assignment expression, often to assign fields.
     * "this" can also be used in a non-constructor method, since the object
     * will have been initialized by then.
     */
	static class ThisDotAssign {
        public ThisDotAssign at;
        ThisDotAssign() {
            this.at = null;
        }
        
        public void notConstructor() {
        	new Receiver(this);
        }
    }
	
    /**
     * Classes whose constructors do not use "this" at all should throw no
     * errors.
     */
	static class Receiver {
		public Receiver(ThisDotAssign var) {}
	}

    static class ConstructorDoesNotUseThis {
        public String s;
        public int i;

        ConstructorDoesNotUseThis() {
            s = "000";
            i = 100;
        }
    }
}
