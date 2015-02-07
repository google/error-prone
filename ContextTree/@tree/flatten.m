function levelContent = flatten(obj)
%% FLATTEN  Return an unordered list of node IDs arranged per level

    depth = obj.depth + 1;
    levelContent = cell( depth, 1 );
    
    levelContent{1} = 1; % Only root at this level
    
    for level = 2 : depth
        parse(level);
    end
    
    
    function parse(level)
        
        levelAbove = levelContent{level-1};
        nParentNodes = numel(levelAbove);
        
        IDcell = cell(nParentNodes, 1);
        for i = 1 : nParentNodes
           
            IDcell{i} = obj.getchildren( levelAbove(i) );
            
        end
        
        levelContent{level} = [ IDcell{:} ];
        
    end
    
    


end