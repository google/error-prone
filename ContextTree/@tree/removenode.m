function obj = removenode(obj , node)

    if node <= 1
        error('MATLAB:tree:removenode', ...
            'Cannot remove root node')
    end

    parents = obj.Parent;
    nodes = obj.Node;
    
    % Attach subtree to new parent node
    children = obj.getchildren(node);
    parent = obj.getparent(node);
    for c = children'
       parents(c) = parent; 
    end
    
    % Remove node
    parents(node) = [];
    nodes(node) = [];
    
    % Shift subsequent indices
    toShift = parents > node;
    parents( toShift ) = parents( toShift ) - 1;
    
    obj.Parent = parents;
    obj.Node = nodes;

end