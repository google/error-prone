package com.softconex.gep.testdata;

import java.util.LinkedList;

public class MyLinkedList<T> extends LinkedList<T> {
	
	public static void main(String[] args) {
		MyLinkedList<String> list = new MyLinkedList<>();
		list.add("abc");
		// BUG: Diagnostic contains: java.util.LinkedList accessed through via index
		list.get(0);
	}
	
}
