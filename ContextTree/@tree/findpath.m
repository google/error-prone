function path = findpath(obj, n1, n2)
%% FINDPATH  Shortest path between two nodes
% PATH = T.FINDPATH(N1, N2)  return the sequence of indices that iterates
% through the tree T from the node of index N1 to the node of index N2.
%
% The path returned is the shortest one in terms of number of edges. It
% always starts with index N1 and ends with index N2. Such a path always
% exists since all nodes are connected in a tree.
%
% EXAMPLE:
% % Find the path between node 'ABplp' and node 'Ca'
% lineage = tree.example;
% n1 = find(lineage.strcmp('ABplp'));
% n2 = find(lineage.strcmp('Ca'));
% path = lineage.findpath(n1, n2)
% pt = tree(lineage, 'clear');
% index = 1;
% for i = path
%   pt = pt.set(i, index);
%   index = index + 1;
% end
% disp(pt.tostring)

    if n1 == n2
        
        path = n1;
        
    elseif any( n2 == obj.depthfirstiterator(n1) )
        % n2 is in the children of n1
        path = [ n1 descend(n1) ];
        
    else
        % n2 is elsewhere in the tree
        path = [ n1 ascend(n1) ];
        
    end
    
    
    function p = ascend(n)
       
        if n == n2 
            p = [];
            return;
        end
        
        parent = obj.getparent(n);
        if any( n2 == obj.depthfirstiterator(parent) )
            % it is in the parent descendant, so descend from the parent
            p = [ parent descend(parent) ];
            
        else
            % no, so we still have to go up
            p = [ parent ascend(parent) ];
            
        end
        
    end
    
    
    function p = descend(n)
        
        if n == n2
            p = [];
            return
        end
        
        children = obj.depthfirstiterator(n);
        for c = children(2 : end) % Get rid of current node in the sequence
            if any( n2 == obj.depthfirstiterator(c) )
                p = [ c descend(c) ];
                break
            end
        end
        
    end


end