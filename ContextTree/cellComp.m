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