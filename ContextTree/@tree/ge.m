function obj = ge(obj, val)
%% >=  Tree element-wise greater-than-or-equal-to comparison, with scalar expansion
    obj = treefun2(obj, val, @ge);
end