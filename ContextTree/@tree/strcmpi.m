function TF = strcmpi(obj, str)
%STRCMPI Compare two trees using string comparison, ignoring case.
%   TF = STRCMP(T1,T2) compares the two synchronized trees T1 and T2 when
%   their content is made of strings. It returns a new synchronized tree,
%   with logical 1 (true) for node indices that store identical strings,
%   except for case, and 0 otherwise.
%
%   If T2 is a single string, it is matched against all the nodes of the T1
%   tree.
%
%   STRCMP supports international character sets.
%
%    See also TREE/STRCMP, TREE/STRNCMP, TREE/STRNCMPI, TREE/REGEXPI

    [obj, str] = permuteIfNeeded(obj, str);

    if isa(str, 'tree')
        
        if ~obj.issync(str)
            error('MATLAB:tree:strcmpi', ...
                'The two trees are not synchronized');
        else
            K = strcmpi(obj.Node, str.Node);
        end
        
    else
        
        K = strcmpi(obj.Node, str);
        
    end
    
    TF = tree(obj, 'clear');
    TF.Node = num2cell(K');

end