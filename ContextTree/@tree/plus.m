function obj = plus(obj, val)
%% +  Add the content of two trees, with scalar expansion
    obj = treefun2(obj, val, @plus);
end