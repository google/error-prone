function [common, length] = condense(obj)
%%CONDENSE Build a new tree by condensing connected with identical value.
%
% [vt, lt] = condense(t) returns two new trees representing a condensed
% version of the specified source tree. Condensation is done by iterating
% down to the source tree, and creating a new node in the two target trees
% only if the source node value differs from its parent.
%
% vt is the tree that contains the common values after condensation.
% lt is the tree that contains the number of nodes that were condensed in
% the source tree.
%
% See also TREE/DECONDENSE
% Jean-Yves Tinevez - 2013


common = tree();
length = tree();

condenseBranch(1, 0);

    function targetParentNode = condenseBranch(sourceNode, targetParentNode)
        
        previousValue = obj.get(sourceNode);
        branchLength = 0;
        
        it = obj.depthfirstiterator(sourceNode);
        for i = it
            
            % See if we can condense the current node.
            currentValue = obj.get(i);
            if isEqual(currentValue, previousValue)
                % Same value, we can condense this node
                branchLength = branchLength + 1;
                
            else
                % New value, we need to create a new node in the target.
                % First we update two target trees with the previous values
                length = length.addnode(targetParentNode, branchLength);
                [common, targetParentNode] = common.addnode(targetParentNode, previousValue);
                % And we go on
                previousValue = currentValue;
                branchLength = 1;
            end
            
            % Now, do we have branching?
            children = obj.getchildren(i);
            if numel(children) > 1
                % Yes. Update the two target trees
                length = length.addnode(targetParentNode, branchLength);
                [common, targetParentNode] = common.addnode(targetParentNode, previousValue);
                
                % Treat each new discovered branch separately
                for j = children
                    condenseBranch(j, targetParentNode);
                end
                % And leave
                return;
                
            end
            
            % Are we done with this branch?
            if obj.isleaf(i)
                % Yes. Update length tree
                length = length.addnode(targetParentNode, branchLength);
                [common, targetParentNode] = common.addnode(targetParentNode, previousValue);

                return;
            end
            
            % No, then we continue going down that branch.
            
        end
        
    end

    function eq = isEqual(a, b)
        if numel(a) ~= numel(b)
            eq = false;
            return;
        end
        if ndims(a) ~= ndims(b)
            eq = false;
            return;
        end
        if any( size(a) ~= size(b) )
            eq = false;
            return;
        end
        if any( a ~= b )
            eq = false;
            return;
        end
        eq = true;
        
    end



end