%%%% Runs a test on the contextTree function
clear
Calltree = contextTree('./Data/input.txt');

%Given an empty stack structure, gives all the stacks in the Calltree stack
stack = {};
stacks = stackCall(stack, Calltree{1}, Calltree); %%Calltree{1} is the parent 

%Given an empty stacks_stats array, it fills it
stacks_stats = {};
[ stacks_stats ] = passfailStats( stacks_stats, stacks, false );




