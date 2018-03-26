package com.google.errorprone.bugpatterns.refactoringexperiment.collect;

import com.google.errorprone.bugpatterns.refactoringexperiment.DataFilter;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreeScanner;


/**
 * Created by ameya on 3/24/18.
 */
public class PrimitiveUsageCollector extends TreeScanner<Void, Void>
        implements TreeVisitor<Void, Void>  {
    int primitiveWrapperUsageCounter = 0;
    @Override
    public Void visitMethodInvocation(MethodInvocationTree mi, Void v) {
        if (DataFilter.isPrimitiveWrapperType(ASTHelpers.getReceiver(mi))){
            primitiveWrapperUsageCounter ++;
        }
        return null;
    }
}
