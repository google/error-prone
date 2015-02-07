function [st, index] = subtree(obj, node, condition)
%%SUBTREE Returns the sub-tree made of all the nodes below the given one.
%
% st = subtree(obj, node) returns a new tree made of all the nodes found
% under the specified node.
%
% st = subtree(obj, node, fun), where fun is an anonymous function that
% takes one argument and returns a boolean, builds a subtree with only the
% nodes whose content satisfy fun(content) = true.
%
% [st, index] = subtree(...) also returns index, which is tree coordinated
% with st, and whose content is the node index in the source tree that was
% read to build the subtree. In practice, st, index and obj are such that:
%
%   st.get(i) == obj.get( index.get(i) )
%
% EXAMPLE:
%
%  ex = tree.example;
%  P1node = ex.strcmp('P1').find; % Find the node index of 'P1'
%  fun = @(x) strncmp(x,'P',1); 
%  st = ex.subtree(P1node, fun);
%  st.tostring % Traverses the P* nodes only
%
% Jean-Yves Tinevez - 2013

    if nargin < 3
        condition = @(s) true;
    end

    % Get indices of the subtree.
    iterator = obj.conditioniterator(node, condition);
    
    % Copy the content of the tree related to the subtree
    parents = obj.Parent(iterator);
    nodes = obj.Node(iterator);
    
    % Revamp parent indices
    newParents = NaN(numel(parents), 1);
    newParents(1) = 0; % The new root node
    for i = 2 : numel(parents)
        pr = parents(i);
        newParents(i) = find( iterator == pr, 1, 'first');
    end
    
    % Create a new tree with the sub-content
    st = tree;
    st.Node = nodes;
    st.Parent = newParents;
    
    % Return the link new node index -> old node index
    index = tree; %iterator;
    index.Parent = newParents;
    index.Node = num2cell(iterator);

end