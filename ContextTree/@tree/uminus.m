function obj = uminus(obj)
%% -  Unary minus.
    obj = obj.treefun(@uminus);
end