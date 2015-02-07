function obj = le(obj, val)
%% <=  Tree element-wise lower-than-or-equal-to comparison, with scalar expansion.
    obj = treefun2(obj, val, @le);
end