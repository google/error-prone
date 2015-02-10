function str = tostring(obj, sorted)
%% TOSTRING  Return a char matrix reprensenting the tree.
% The symbol 'ø' is used for nodes that have no or empty content.
%
% obj.tostring(sorted) sort the nodes in the generated string if 'sorted'
% is true. It is false by defualt.

    if nargin < 2
        sorted = false;
    end

    %% Use a tree to compute spaces;

    % 1. Generate string representation of content as a tree
    strContentTree = obj.treefun(@contentToString);
    
    % 1.25 Compute space requirements
    spaceTree = strContentTree.treefun(@numel);
    
    % 1.5 Each children must at least have a size determined by its parent
    iterator = spaceTree.depthfirstiterator;
    for i = iterator
       parent = spaceTree.getparent(i);
       if parent == 0
           continue
       end
       
       nSiblings = numel(spaceTree.getsiblings(i));
       parentWidth = spaceTree.get(parent);
       minWidth = ceil(parentWidth / nSiblings);
       thisWidth = spaceTree.get(i);
       if thisWidth < minWidth
           spaceTree = spaceTree.set(i, minWidth);
       end
    end
    
    % 2. Add 2 for proper spacing
    spaceTree = spaceTree + 2;
    
    % 3. Build cumulative space tree
    spaceTree = spaceTree.recursivecumfun(@sum);
    
    % Put at least 1 when there is nothing
    iterator = spaceTree.depthfirstiterator;
    for i = iterator
       if isempty(spaceTree.get(i)) || spaceTree.get(i) == 0
           spaceTree = spaceTree.set(i, 1);
       end
    end
    
    %% Iterate in the tree a first time to put the node content and the vertical bars
    
    nLevels = obj.depth + 1;
    depth = obj.depthtree;
    
    iterator = obj.depthfirstiterator(1, sorted);
    
    str = repmat(' ', 3 * nLevels - 2, spaceTree.get(1));
    
    % The last column in which something was writen in at given level.
    columns = zeros(nLevels, 1);
    % And a matching tree to store at what position we write. We will need
    % this in the second iteration.
    columnTree = tree(obj);
    
    for i = 1 : numel(iterator)
        
        ID = iterator(i);
        level = depth.get(ID) + 1; % Because the 2 tree have the same structure
        spaceWidth = spaceTree.get(ID);
        ds = ceil( spaceWidth / 2 );

        index = columns(level) + ds;
        row = 3 * (level-1) + 1;

        if level > 1
            % Line 0: the '+'
            if numel(obj.getsiblings(ID)) > 1
                ch = '+';
            else
                ch = '|';
            end
            str(row-2, index) = ch;
            
            % Line 1: the vertical tick
            str(row-1, index) = '|';
            
        end
        
        % Line 2: the content
        contentStr = strContentTree.get(ID);
       
        contentWidth = numel(contentStr);
        dc = floor(contentWidth / 2);
        
        str(row, index-dc : index + contentWidth-1-dc) = contentStr;
        
        columnTree = columnTree.set(ID, columns(level));
        columns(level) = columns(level) + spaceWidth;
        
        % Is a leaf? Then we move the cursor to the right for the next
        % levels. If we do not do it, the display is going to be messy as
        % soon as the nodes do not have the same number of children.
        
        if obj.isleaf(ID) && level < nLevels
            
            for j = level+1 : nLevels
                columns(j) = columns(j) + spaceWidth;
            end
            
        end
        
    end
    
    %% A second iteration to draw horizontal bars
    
    for i = 1 : numel(iterator)
        
        ID = iterator(i);
        level = depth.get(ID) + 1; % Because the 2 tree have the same structure
         if level == nLevels || obj.isleaf(ID)
            continue
        end
        
        spaceWidth = spaceTree.get(ID);
        ds = floor( spaceWidth / 2 );
        col = columnTree.get(ID);
        row = 3 * (level-1) - 1;
        
        % Move to the level below, and edit the line with the '+' so that
        % they show siblings
        
        index = col + ds;
        if numel(obj.getchildren(ID)) > 1
            ch = '+';
        else
            ch = ' ';
        end
        str(row+3, index) = ch;
        
        childbar = str(row+3, col + 1 : col + spaceWidth);
        ai = find(childbar == '+' | childbar == '|');

        if isempty(ai)
            continue
        end
        
        fi = ai(1);
        li = ai(end);
        
        toReplace = setdiff( fi:li, ai);
        childbar(toReplace) = '-';
        str(row+3, col + 1 : col + spaceWidth) = childbar;
        
    end
    
    
end