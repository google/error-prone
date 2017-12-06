package com.softconex.gep.testdata;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AddAllLinkedList {

    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        list.add("abc");
        // BUG: Diagnostic contains: java.util.LinkedList accessed through via index
        list.addAll(1, Arrays.asList("456", "xyz"));
    }
}
