function obj = graft(obj, ID, othertree)
%% GRAFT   Graft another tree at the specified node of this tree.

    
    nNodes = numel(obj.Parent);

    otParents = othertree.Parent;
    % Shift other parent indices
    otParents = otParents + nNodes;
    % Make the other root a child of the target node
    otParents(1) = ID;
    
    % Concatenate
    newParents = [ obj.Parent ; otParents ];
    newNodes   = vertcat( obj.Node, othertree.Node );
    
    % Edit
    obj.Node = newNodes;
    obj.Parent = newParents;
    

end