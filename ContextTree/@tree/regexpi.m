function ktree = regexpi(obj, pattern, varargin)
%REGEXPI Match regular expression for tree content, ignoring case.
%   KTREE = REGEXPI(T,EXPRESSION) matches the regular expression,
%   EXPRESSION, in the tree T content.  The indices of the beginning of the
%   matches are returned in a new synchronized tree.
%
%   This tree version of REGEXPI supports the 6 outputs of the core REGEXPI
%   function. You can call KTREE = REGEXPI(T,EXPRESSION,KEYWORD), with
%   KEYWORD an option string or multiple option strings taken from the
%   following list:
%
%          Keyword   Result
%   ---------------  --------------------------------
%          'start'   Row vector of starting indices of each match
%            'end'   Row vector of ending indices of each match
%   'tokenExtents'   Cell array of extents of tokens in each match
%          'match'   Cell array of the text of each match
%         'tokens'   Cell array of the text of each token in each match
%          'names'   Structure array of each named token in each match
%          'split'   Cell array of the text delimited by each match
%
%   KTREE will then be an array of tree objects, one per required inputs.
%   Each element of the array is a tree that hold the matching keyword
%   result.
%
%   This tree version of REGEXPI does not support any other options.
%   
%   See also REGEXPI, TREE/REGEXP, TREE/REGEXPREP, REGEXPTRANSLATE, TREE/STRCMP, TREE/STRFIND.
%
    
    %% CONSTANTS

    ACCEPTED_OPTIONS = {
        'start'
        'end'
        'tokenExtents'
        'match'
        'tokens'
        'names'
        'split'
        };

    %% CODE
    
    nout = numel(varargin);
    
    if nout == 0
        ktree = tree(obj, 'clear');
        ktree.Node = regexpi(obj.Node, pattern);
        
    else

        ktree(nout, 1) = tree;
        for i = 1 : nout
            
            option = varargin{i};
            if all(isempty(strcmp(ACCEPTED_OPTIONS, option)))
               error('MATLAB:tree:regexpi', ...
                   'Unknown option: %s.', option)
            end
            
            ktree(i) = tree(obj, 'clear');
            ktree(i).Node = regexpi(obj.Node, pattern, option);
            
        end
        
    end


end