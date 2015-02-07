function obj = times(obj, val)
%% .*  Tree element-wise multiplication, with scalar expansion
    obj = treefun2(obj, val, @times);
end