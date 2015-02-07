function obj = lt(obj, val)
%% <  Tree element-wise less-than comparison, with scalar expansion
    obj = treefun2(obj, val, @lt);
end