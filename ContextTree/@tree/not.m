function obj = not(obj)
%% ~  Tree element-wise logical NOT.
    obj = obj.treefun(@not);
end