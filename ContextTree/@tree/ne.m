function obj = ne(obj, val)
%% ~=  Tree element-wise not-equal-to comparison, with scalar expansion.
    obj = treefun2(obj, val, @ne);
end