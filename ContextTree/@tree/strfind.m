function ktree = strfind(obj, pattern)
%STRFIND Find one string within the content of the tree.
%   KTREE = STRFIND(T,PATTERN) returns the starting indices of any 
%   occurrences of the string PATTERN in the content of the tree T.
%
%   Results are returned in a new synchronized tree, where each node is an
%   array containind the starting index of any occurence of PATTERN found
%   in the corresponding node. Internally, this method is based on the
%   regular strfind function, so the syntax applies here as well.
%
%   EXAMPLE
%       lineage = tree.example; 
%       ktree = lineage.strfind('a');
%       disp(ktree.tostring)
%
%   See also TREE/STRCMP, TREE/STRNCMP, TREE/REGEXP.

    ktree = tree(obj, 'clear');
    ktree.Node = strfind(obj.Node, pattern);

end