function obj = chop(obj, node)
%% CHOP  Remove the target node and all subnodes from the given tree.

    iterator = obj.depthfirstiterator(node);
    
    % Build new parent array
    np = obj.Parent;
    
    % Remove unwanted nodes
    np ( iterator ) = [];
    
    % Shift parent value: if a parent were after some nodes we removed, we
    % need to shift its value by an amount equal to the number of parent we
    % removed, and that were BEFORE the target parent
    for i = 1 : numel(np)
        np(i) = np(i) - sum(np(i) > iterator);
    end
    
    obj.Parent = np;
    obj.Node(iterator) = [];

end