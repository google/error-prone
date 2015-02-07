function TF = strncmpi(obj, str, n)
%%STRNCMPI Tree comparison over first N characters, case insensitive.
%   TF = STRCMPI(T1,T2,N) compares the two synchronized trees T1 and T2
%   when their content is made of strings. It returns a new synchronized
%   tree, with logical 1 (true) for node indices that store strings that
%   have theit first N characters identical, ignoring case, and 0
%   otherwise.
%
%   If T2 is a single string, it is matched against all the nodes of the T1
%   tree.
%
%   STRNCMPI supports international character sets.
%
%   See also TREE/STRCMP, TREE/STRNCMP, TREE/STRCMPI, TREE/REGEXPI

    [obj, str] = permuteIfNeeded(obj, str);

    if isa(str, 'tree')
        
        if ~obj.issync(str)
            error('MATLAB:tree:strcmp', ...
                'The two trees are not synchronized');
        else
            K = strncmpi(obj.Node, str.Node, n);
        end
        
    else
        
        K = strncmpi(obj.Node, str, n);
        
    end
    
    TF = tree(obj, 'clear');
    TF.Node = num2cell(K');

end