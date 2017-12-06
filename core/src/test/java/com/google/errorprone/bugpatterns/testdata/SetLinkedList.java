package com.softconex.gep.testdata;

import java.util.LinkedList;
import java.util.List;

public class SetLinkedList {

    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        // BUG: Diagnostic contains: java.util.LinkedList accessed through via index
        list.set(0, "abc");
    }
}
