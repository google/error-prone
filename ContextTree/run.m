%%%% Runs a test on the contextTree function
clear
Calltree = contextTree('./Data/input.txt');
stack = {};
stacks = stackCall(stack, Calltree{1}, Calltree); %%Calltree{1} is the parent 




