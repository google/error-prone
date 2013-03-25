/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import java.util.*;
import com.google.common.base.Objects;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
public class  IncompatibleEqualsNegativeCases {


    public boolean testEquality(Object o, String s1, String s2, Integer i) {

        if (o.equals(s1)) 
            return true;
        if (s1.equals(o)) 
            return true;
        if (s1.equals(s1))
            return true;
        if (i.equals(17))
            return true;
       
        return false;
    }
    
    public boolean testGuavaEqual(Object o, String s1, String s2, Integer i) {

        if (Objects.equal(o, s1)) 
            return true;
        if (Objects.equal(s1, o)) 
            return true;
        if (Objects.equal(s1, s1))
            return true;
        if (Objects.equal(i, 17))
            return true;
       
        return false;
    }
 
    public void testAssertFalse(String s, Integer i) {
        assertFalse(s.equals(i));
    }
    
    public boolean testCollection(Collection<String> c, HashSet<String> s, TreeSet<String> s2, Set<String> s3,  Object o) {
        if (c.equals(s))
            return true;
        if (s.equals(c))
            return true;
        if (o.equals(c))
            return true;
        if (s.equals(s2))
            return true;
        if (s2.equals(s3))
            return true;
        
        return false;
    }
    public boolean testCollectionWithGuava(Collection<String> c, HashSet<String> s, TreeSet<String> s2, Set<String> s3,  Object o) {
        if (Objects.equal(c, s))
            return true;
        if (Objects.equal(s, c))
            return true;
        if (Objects.equal(o, c))
            return true;
        if (Objects.equal(s, s2))
            return true;
        if (Objects.equal(s2, s3))
            return true;
        
        return false;
    }
    
    public void assertFalse(boolean b) {
        if (b) throw new AssertionError();
    }
    
    public static class DifferentClassesButMightBeEqual {
        int value;

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object x) {
            if (!(x instanceof DifferentClassesButMightBeEqual))
                return false;
            return value == ((DifferentClassesButMightBeEqual) x).value;
        }

        static class One extends DifferentClassesButMightBeEqual {
        };

        static class Two extends DifferentClassesButMightBeEqual {
        };

        public static void foobar() {
            One one = new One();
            Two two = new Two();
            System.out.println(one.equals(two));
        }

    }

}
