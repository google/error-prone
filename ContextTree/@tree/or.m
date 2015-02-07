function obj = or(obj, val)
%% |  Tree element-wise logical OR, with scalar expansion.
    obj = treefun2(obj, val, @or);
end