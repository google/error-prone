function iterator = breadthfirstiterator(obj, sorted)
%%BREADTHFIRSTITERATOR  Index sequence traversing the tree, breadth first.
%
% it = tree.BREADTHFIRSTITERATOR returns a line vector of indices that
% traverses the tree by increasing depth (breadth-first).
%
% it = tree.BREADTHFIRSTITERATOR(sorted) arranges the indices by sorted
% order. Sorting is done using the MATLAB function 'sortrows'.
%
% EXAMPLE
%
%   extree = tree.example;
%   it = extree.breadthfirstiterator(true);
%   for i = it
%       disp(extree.get(i));
%   end
% 
% Jean-Yves Tinevez, 2013

    if nargin < 2
        sorted = false;
    end

    f = obj.flatten;
    
    if sorted
       for i = 1 : numel(f)
           nodes = f{i};
           contents = obj.Node(nodes);
           [ ~, sorting_array ] = sortrows(contents);
           nodes = nodes(sorting_array);
           f{i} = nodes;
       end
    end
    
    iterator = [ f{:} ];

end