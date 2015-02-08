%% Author: Pedro Borges
% Receives the directories containing files with output logs for passed
% tests and failed tests. Returns each stack annotated with the number of
% times it was encounter on a pass and on a fail test. Also returns the
% total number of passed tests and te total number of failed tests
function [ stacks_stats, Totalpasses, Totalfails ] = statsMultipleCalls( Dir_failed, Dir_passed )

passed_dir = dir(Dir_passed);
failed_dir = dir(Dir_failed);

%Given an empty stacks_stats array, it fills it
stacks_stats = {};

Totalpasses = 0; %Initializing
%%% Loop for the passed directory
for i = 1:length(passed_dir)
    istxt = strfind( passed_dir(i).name, '.txt');
    iscct = strfind( passed_dir(i).name, '.cct');
    if ~isempty(istxt) || ~isempty(iscct)
        Totalpasses = Totalpasses +1;
        file = strcat(Dir_passed, passed_dir(i).name)
        
        Calltree = contextTree(file);
        %Given an empty stack structure, gives all the stacks in the Calltree stack
        stack = {};
        stacks = stackCall(stack, Calltree{1}, Calltree); %%Calltree{1} is the parent
        [ stacks_stats ] = passfailStats( stacks_stats, stacks, true );
        %%% If the passes array stacks_stats is not empty, then it adds to it
    end
end


Totalfails = 0;
%%% Loop for the failed directory
for i = 1:length(failed_dir)
    istxt = strfind( failed_dir(i).name, '.txt');
    iscct = strfind( failed_dir(i).name, '.cct');
    if ~isempty(istxt) || ~isempty(iscct)
        Totalfails = Totalfails+1;
        file = strcat(Dir_failed, failed_dir(i).name)
        Calltree = contextTree(file);
        %Given an empty stack structure, gives all the stacks in the Calltree stack
        stack = {};
        stacks = stackCall(stack, Calltree{1}, Calltree); %%Calltree{1} is the parent
        [ stacks_stats ] = passfailStats( stacks_stats, stacks, false );
        %%% If the passes array stacks_stats is not empty, then it adds to it
    end
end



end

