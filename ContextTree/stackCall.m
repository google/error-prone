%%%% Uses a tree and forms the stacks of calls from it.

function [stacks] = stackCall(stack, node, Calltree)

if ~isempty(node)
    
    stack = push(stack, node.name);
    if ~isempty(node.child) %% If the node has children
        stacks = {};
        for i = 1:length(node.child)
           aux = stackCall(stack,Calltree{node.child(i)}, Calltree);
           stacks = [stacks, aux];
        end
        
    else
        %%%% If there are no children, it returns the current stack
    stacks = {stack};
    end
    
    
end

% pedro = {};
% pedro = push(pedro, 'one');
% pedro = push(pedro, 'two');
% [pedro, element] = pop(pedro);
% assert;
end

function [stack] = push(stack, element)

stack = [stack; element];

end

function [stack, element] = pop(stack)

element = stack(end);
stack(end) = [];

end