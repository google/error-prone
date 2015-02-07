function flag = all(obj)
%% ALL  Return true if and only if all the nodes content are true.
    flag = all([obj.Node{:}]);
end