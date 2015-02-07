function ie = isemptynode(obj)
%% ISEMPTYNODE  Return a tree where each node is true iff the matching tree node is empty

    ie = tree(obj, 'clear');
    ie.Node = cellfun(@isempty, obj.Node, 'UniformOutput', false);

end