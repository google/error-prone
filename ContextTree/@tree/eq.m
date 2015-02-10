function obj = eq(obj, val)
%% ==  Tree element-wise equality comparison, with scalar expansion.
    obj = treefun2(obj, val, @eq);
end