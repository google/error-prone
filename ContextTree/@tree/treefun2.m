function obj = treefun2(obj, val, fun)
%%TREEFUN2  Two-arguments function on trees, with scalar expansion.
    
    [obj, val] = permuteIfNeeded(obj, val);

     if isa(val, 'tree')
       
        content = cellfun(fun, obj.Node, val.Node, ...
            'UniformOutput', false);
        
     else
         
         content = cellfun(@(x) fun(x, val), obj.Node, ...
            'UniformOutput', false);
    
     end
    
     obj.Node = content;
end