function obj = minus(obj, val)
%% -  Subtract the content of two trees, with scalar expansion
    obj = treefun2(obj, val, @minus);
end