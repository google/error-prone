function ktree =  regexprep(obj, expression, replace, varargin)
%REGEXPREP Replace string using regular expression.
%   KTREE = REGEXPREP(T,EXPRESSION,REPLACE) replaces all occurrences of the
%   regular expression, EXPRESSION, in the content of the tree T, with the
%   string REPLACE. A new synchronized tree is returned with the new
%   content.  If no matches are found REGEXPREP returns the initial content
%   unchanged.
%
%   By default, REGEXPREP replaces all matches and is case sensitive.
%   Available options are:
%
%           Option   Meaning
%   ---------------  --------------------------------
%     'ignorecase'   Ignore the case of characters when matching EXPRESSION to
%                       the three content.
%   'preservecase'   Ignore case when matching (as with 'ignorecase'), but
%                       override the case of REPLACE characters with the case of
%                       corresponding characters in the tree content when replacing.
%           'once'   Replace only the first occurrence of EXPRESSION in each node content.
%               N    Replace only the Nth occurrence of EXPRESSION in each node content.
%
%   REGEXPREP can modify REPLACE using tokens from EXPRESSION.  The 
%   metacharacters for tokens are:
%
%    Metacharacter   Meaning
%   ---------------  --------------------------------
%              $N    Replace using the Nth token from EXPRESSION
%         $<name>    Replace using the named token 'name' from EXPRESSION
%              $0    Replace with the entire match
%
%   To escape a metacharacter in REGEXPREP, precede it with a '\'.
%
%   REGEXPREP supports international character sets.
%
%   EXAMPLE:
%
%       % Replace the 'l' and 'r' with 'left' and 'right'
%       lineage = tree.example;
%       ktree = lineage.regexprep({'l' 'r'}, {'-left-' '-right'});
%       disp(ktree.tostring)
%
%   See also REGEXPREP, TREE/REGEXP, TREE/REGEXPI, REGEXPTRANSLATE.
%

    ktree = tree(obj, 'clear');
    ktree.Node = regexprep(obj.Node, expression, replace, varargin{:});


end