function obj = gt(obj, val)
%% >  Tree element-wise greater-than comparison, with scalar expansion
    obj = treefun2(obj, val, @gt);
end