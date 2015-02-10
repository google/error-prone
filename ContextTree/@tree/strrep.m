function ktree =  strrep(obj, oldstr, newstr)
%STRREP Modify tree content by replacing a string with another.
%   KTREE = STRREP(T,OLDSTR,NEWSTR) replaces all occurrences of the string
%   OLDSTR within all the nodes of the tree T, provided they are all
%   strings, with the string NEWSTR. Results are returned as a new
%   synchronized tree.
%
%
%   See also STRREP, TREE/REGEXPREP
%

    ktree = tree(obj, 'clear');
    ktree.Node = strrep(obj.Node, oldstr, newstr);


end