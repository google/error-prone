%%  Author: Pedro Borges
%%Function Receives the stacks and their respective stats and write them to
%%two csv filed. One file contain the stacks. The other file contain their
%%stats. The stats are number of passed tests and number of failed tests
function [  ] = writeResults(stacks_stats )
file_stacks = './Results/stacks.csv';
file_stats = './Results/stats.csv';
fid_stacks = fopen(file_stacks, 'w');
fid_stats = fopen(file_stats, 'w');

for i = 1:length(stacks_stats)
    %%Writes the index in the first colum
    fprintf(fid_stacks, '%d', i); 
    fprintf(fid_stats, '%d,%d,%d', i, stacks_stats(i).passes, stacks_stats(i).fails);
    for j = 1:length(stacks_stats(i).stack{1}) %% have to put the {1}
        fprintf(fid_stacks, ',%s', char(stacks_stats(i).stack{1}(j))); %% Writes the stacks to one file  
    end
    fprintf(fid_stacks, '\n');
    fprintf(fid_stats, '\n');
end

fclose(fid_stacks);
fclose(fid_stats);
end

