function [obj val] = permuteIfNeeded(obj, val)
    if ~isa(obj, 'tree')
       tmp = obj;
       obj = val;
       val = tmp;
    end
end