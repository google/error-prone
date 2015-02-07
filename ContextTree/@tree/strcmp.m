function TF = strcmp(obj, str)
%STRCMP Compare two trees using string comparison.
%   TF = STRCMP(T1,T2) compares the two synchronized trees T1 and T2 when
%   their content is made of strings. It returns a new synchronized tree,
%   with logical 1 (true) for node indices that store identical strings and
%   0 otherwise.
%
%   If T2 is a single string, it is matched against all the nodes of the T1
%   tree.
%
%   STRCMP supports international character sets.
%
%   See also TREE/STRNCMP, TREE/STRCMPI, TREE/STRFIND, TREE/REGEXP

    [obj, str] = permuteIfNeeded(obj, str);

    if isa(str, 'tree')
        
        if ~obj.issync(str)
            error('MATLAB:tree:strcmp', ...
                'The two trees are not synchronized');
        else
            K = strcmp(obj.Node, str.Node);
        end
        
    else
        
        K = strcmp(obj.Node, str);
        
    end
    
    TF = tree(obj, 'clear');
    TF.Node = num2cell(K');

end