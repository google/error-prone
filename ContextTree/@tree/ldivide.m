function obj = ldivide(obj, val)
%% .\  Tree left element-wise division, with scalar expansion
    obj = treefun2(obj, val, @ldivide);
end