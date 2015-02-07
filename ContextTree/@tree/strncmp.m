function TF = strncmp(obj, str, n)
%STRNCMP Compare two trees using string comparison over first N characters.
%   TF = STRCMPN(T1,T2,N) compares the two synchronized trees T1 and T2
%   when their content is made of strings. It returns a new synchronized
%   tree, with logical 1 (true) for node indices that store strings that
%   have theit first N characters identical, and 0 otherwise.
%
%   If T2 is a single string, it is matched against all the nodes of the T1
%   tree.
%
%   STRNCMP supports international character sets.
%
%    See also TREE/STRCMP, TREE/STRNCMPI, TREE/STRNCMPI, TREE/REGEXPI

    [obj, str] = permuteIfNeeded(obj, str);

    if isa(str, 'tree')
        
        if ~obj.issync(str)
            error('MATLAB:tree:strcmp', ...
                'The two trees are not synchronized');
        else
            K = strncmp(obj.Node, str.Node, n);
        end
        
    else
        
        K = strncmp(obj.Node, str, n);
        
    end
    
    
    TF = tree(obj, 'clear');
    TF.Node = num2cell(K');

end