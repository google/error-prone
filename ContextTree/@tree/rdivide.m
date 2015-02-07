function obj = rdivide(obj, val)
%% ./  Tree rigth element-wise division, with scalar expansion
    obj = treefun2(obj, val, @rdivide);
end