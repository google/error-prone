function flag = issync(obj, anotherobj)
%%ISSYNC Returns true if the two trees are coordinated.
% Two trees are coordinated if they have the same structure and the same
% iteration order, but possibly different node content.
    if numel(obj.Parent) ~= numel(anotherobj.Parent)
        flag = false;
    else
        flag = all (  obj.Parent == anotherobj.Parent );
    end

end