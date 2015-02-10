function obj = power(obj, val)
%% .^  Tree element-wise power, with scalar expansion
    obj = treefun2(obj, val, @power);
end