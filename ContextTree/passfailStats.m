%%  Author: Pedro Borges
%%%
%%%Receives a vector of structure of the type stacks_stats with previous
%%% stats, a vector of stacks and if the test was a pass or fail. Returns
%%% the updated stacks_stats vector with the correct number of pass and
%%% fail tests.
%%%Input:
%%%     stacks_stats: structure containing {one stack, number of passes, number of fails}
%%%     stack: Vector containing the stacks of a given test
%%%     passes: number of cumulative passes for that stack
%%%     fails: number of cumulative fails for that stack
%%%Ouput

function [ stacks_stats ] = passfailStats( stacks_stats, stacks, ifpass )

if(isempty(stacks_stats)) %% If it is the first time running this
    stacks_stats(1).stack = {'init'};
    stacks_stats(1).passes = 0;
    stacks_stats(1).fails = 0;
    for i = 1:length(stacks)
        
        stacks_stats = addElement(stacks_stats, stacks(i), ifpass);
    end
else %% If there were already elements in the stacks_stats array
    for i = 1:length(stacks)
        
        index = findStack(stacks(i), stacks_stats); %% find stacks(i) in stacks_stats. Returns 0 or the index where found it.
        
        if index > 0 % means it found it
            stacks_stats = updateElement(stacks_stats, index, ifpass);
        else %% means that the element still does not exist
            stacks_stats = addElement(stacks_stats, stacks(i), ifpass);
        end
        
    end
    
end
end


%%%% Updates element in the vector of stats of the stacks
function [stacks_stats] = updateElement(stacks_stats, index, ifpass )

if ifpass == true
    aux = stacks_stats(index).passes;
    aux = aux +1;
    stacks_stats(index).passes = aux;
else
    aux = stacks_stats(index).fails;
    aux = aux+1;
    stacks_stats(index).fails = aux;
end

end


%%% Stack is only one specific stack
function [stacks_stats] = addElement ( stacks_stats, stack, ifpass )
if (stacks_stats(end).passes == 0) && (stacks_stats(end).fails == 0) %% means it is only the init structure
    stacks_stats(end).stack= stack; % doesn't extend if it is only the init structure
else
    stacks_stats(end+1).stack= stack; %% End +1 here to extend the array. Then only end to not extend it past it
end
if ifpass == true
    stacks_stats(end).passes = 1;
    stacks_stats(end).fails = 0;
else
    stacks_stats(end).fails = 1;
    stacks_stats(end).passes = 0;
end
end

%%%% Searches for the stack in the array of stacks_stats
function [index] = findStack(stack, stacks_stats)
index = 0;
for i = 1:length(stacks_stats)
    
    isequal = cellComp(stack{1}, stacks_stats(i).stack{1}); %% needs the {1} because the array is inside the cell
    if isequal == 1 %% means it found the stack
        index = i; %% index is the current position on the array
        %break;
    end
end
end


%%%% Used to compare two cell arrays
%%%% Returns 0 if the two stacks are differente
%%%% Returns 1 if the two stacks are equal
function [is_equal] = cellComp(stacka, stackb)

if length(stacka) ~= length(stackb)
    
    is_equal = false;
else
    aux = strcmp(stacka,stackb) ;
    aux = aux -1;
    if ~isempty(find(aux))  %% if there is a term that is any -1 term, then they are not equal
        is_equal = false;
    else
        is_equal = true;
    end
    
end
end
