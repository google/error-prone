/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import java.util.Iterator;

/** @author jsjeon@google.com (Jinseong Jeon) */
public class IterableAndIteratorNegativeCases {

  /** Test Node */
  public static class MyNode {
    String tag;
    MyNode next;
  }

  /** Test List that implements only Iterator */
  public static class MyList1 implements Iterator<MyNode> {
    private MyNode head;

    public MyList1() {
      head = null;
    }

    @Override
    public boolean hasNext() {
      return head != null;
    }

    @Override
    public MyNode next() {
      if (hasNext()) {
        MyNode ret = head;
        head = head.next;
        return ret;
      }
      return null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove is not supported");
    }

    public void add(MyNode node) {
      if (!hasNext()) {
        head.next = node;
      }
      head = node;
    }
  }

  /** Test List that implements only Iterable */
  public static class MyList2 implements Iterable<MyNode> {

    @Override
    public Iterator<MyNode> iterator() {
      MyList1 l = new MyList1();
      // code to populate the list goes here
      return l;
    }
  }
}
