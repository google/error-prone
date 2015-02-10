function newTree = treefun(obj, fun)
%% TREEFUN  Create a new tree by applying function handle to each node of the source tree.

    if ~isa(fun, 'function_handle')
        error('MATLAB:tree:treefun', ...
            'Second argument, fun, must be a function handle. Got a %s.', ...
            class(fun));
    end
       
    % First we copy
    newTree = tree(obj, 'clear'); 

    % Then we override
    newTree.Node = cellfun(fun, obj.Node, 'UniformOutput', false);

end