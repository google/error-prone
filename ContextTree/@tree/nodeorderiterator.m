function iterator = nodeorderiterator(obj)
%% NODEORDERITERATOR  Traversing the tree, following how the nodes were added.

    iterator = 1 : numel(obj.Parent);

end