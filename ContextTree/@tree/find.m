function I = find(obj, varargin)
%FIND   Find indices of nonzero nodes in the tree.
%   I = FIND(T) returns the linear indices corresponding to the nonzero
%   entries of the tree T. T may be a tree made of logical scalar elements.
%   
%   I = FIND(T,K) returns at most the first K indices corresponding to 
%   the nonzero entries of the tree T.  K must be a positive integer, 
%   but can be of any numeric type.
%
%   I = FIND(T,K,'first') is the same as I = FIND(T,K).
%
%   I = FIND(T,K,'last') returns at most the last K indices corresponding 
%   to the nonzero entries of the tree T.
%
    val = [ obj.Node{:} ] ;
    I = find(val, varargin{:});
end