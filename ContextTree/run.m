%%  Author: Pedro Borges
%%%% Runs a test on the contextTree function
clear

Calltree = contextTree('./TestInput/input.txt');

%Given an empty stack structure, gives all the stacks in the Calltree stack
stack = {};
stacks = stackCall(stack, Calltree{1}, Calltree); %%Calltree{1} is the parent 






