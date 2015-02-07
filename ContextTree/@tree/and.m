function obj = and(obj, val)
%% &  Tree element-wise logical AND, with scalar expansion.
    obj = treefun2(obj, val, @and);
end