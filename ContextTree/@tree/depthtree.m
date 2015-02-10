function dt = depthtree(obj)
%% DEPTHTREE  Create a coordinated tree where each node holds its depth.
% As for tree.getDepth, the node root has a depth of 0, its children a
% depth of 1, and recursively to the leaves.

    dt = tree(obj);
    dt = dt.set(1, 0);
    
    iterator = obj.depthfirstiterator;
    iterator(1) = []; % Remove root
    
    for i = iterator
        
        parent = dt.Parent(i);
        parentDepth = dt.get(parent);
        dt = dt.set(i, parentDepth+1);
        
    end

end