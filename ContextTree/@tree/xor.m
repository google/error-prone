function obj = xor(obj, val)
%% XOR  Tree element-wise logical exclusive-OR, with scalar expansion.
    obj = treefun2(obj, val, @xor);
end