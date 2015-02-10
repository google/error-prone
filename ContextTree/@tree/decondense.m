function obj = decondense(vt, lt)
%%DECONDENSE Build a new tree by decondensing common value.
%
% t = condense(vt, lt) returns a new tree built by decondensing the two
% specified trees. A node is added to the target tree, with a value taken
% in vt, and readded with the same value a number of time taken in lt.
%
% See also TREE/CONDENSE
% Jean-Yves Tinevez - 2013


obj = tree();

decondenseBranch(1, 0);

    function targetParentNode = decondenseBranch(sourceNode, targetParentNode)
        
        % Decondense the branch
        value = vt.get(sourceNode);
        ntimes = lt.get(sourceNode);
        
        for i = 1 : ntimes
           [obj, targetParentNode] = obj.addnode(targetParentNode, value);
        end
        
        % Recurse into children
        children = vt.getchildren(sourceNode);
        for c = children
            decondenseBranch(c, targetParentNode);
        end
        
        % Then leave
        return;
        
        
    end


end