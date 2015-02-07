function flag = any(obj)
%% ANY  Return true if any node content is nonzero.
    flag = any([obj.Node{:}]);
end