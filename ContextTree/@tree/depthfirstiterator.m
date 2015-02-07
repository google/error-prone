function IDs = depthfirstiterator(obj, startNode, sorted)
%%DEPTHFIRSTITERATOR  Index sequence traversing the tree, depth first.
% 
% iterator = tree.DEPTHFIRSTITERATOR return a line vector of indices that
% traverse the tree in a depth-first manner, starting from the root node.
%
% iterator = tree.DEPTHFIRSTITERATOR(node) traverse the tree, but starting
% at the node of given index, and iterating only through the sub-nodes of
% this node.
%
% iterator = tree.DEPTHFIRSTITERATOR(node, true) iterate through children
% in sorted order when a branching node is met. This is false by default.
% Sorting is done using the MATLAB function 'sortrows'.
%
% EXAMPLE 1
% % Create a copy of the tree that holds the order of iteration
% lineage = tree.example;
% itOrder = tree(lineage, 'clear'); % Copy the tree structure only
% iterator = itOrder.depthFirstIterator;
% index = 1;
% for i = iterator
%   itOrder = itOrder.set(i, index);
%   index = index + 1;
% end
% disp(itOrder.toString)
%
% EXAMPLE 2
% % List the content of a subtree of the tree
% lineage = tree.example;
% iterator = lineage.depthFirstIterator(19);
% content = cell(numel(iterator), 1);
% index = 1;
% for i = iterator
%   content{index} = lineage.get(i);
%   index = index + 1;
% end
% disp(content)

    if nargin < 2
        startNode = 1;
    end
    if nargin < 3
        sorted = false;
    end

    IDs = recurse(startNode);

    function val = recurse(node)
        
        val = node;
        if obj.isleaf(node)
            return
        else
            
           children = obj.getchildren(node);
           
           if sorted && numel(children) > 1
               
               contents = obj.Node(children);
               [~, sorting_array] = sortrows(contents);
               children = children(sorting_array);
               
           end
           
           cellval = cell(numel(children), 1);
           for i = 1 : numel(children)
               cellval{i} = recurse(children(i));
           end
           val = [ val cellval{:} ] ;
           
        end
        
        
        
    end



end