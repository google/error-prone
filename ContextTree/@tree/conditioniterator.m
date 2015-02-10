function IDs = conditioniterator(obj, startNode, condition, sorted)


    if nargin < 2 || isempty(startNode)
        startNode = 1;
    end
    
    if nargin < 3 || isempty(condition)
        condition = @(x) true;
    end
    
    if nargin < 4 || isempty(sorted)
        sorted = false;
    end
    
    IDs = recurse(startNode);

    function val = recurse(node)
        
        val = node;
        content = obj.Node{val};
        if obj.isleaf(node) || ~condition(content)
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
               
               content = obj.Node{children(i)};
               if ~condition(content)
                   continue
               end
                   
               cellval{i} = recurse(children(i));
           end
           val = [ val cellval{:} ] ;
           
        end
        
        
        
    end



end