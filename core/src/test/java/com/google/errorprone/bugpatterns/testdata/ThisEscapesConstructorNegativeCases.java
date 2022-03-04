package com.google.errorprone.bugpatterns.testdata;

public class ThisEscapesConstructorNegativeCases {
	static class ThisDotAssign {
        public ThisDotAssign at;
        ThisDotAssign() {
            this.at = null;
        }
        
        public void notConstructor() {
        	new Receiver(this);
        }
    }
	
	static class Receiver {
		public Receiver(ThisDotAssign var) {}
	}
}
