%%   Author: Pedro Borges
%%%%% Reads Log from file and creates a context tree
%%%%% Input:
%%%%%   File: File containing lines of CALL name and RETURN name
%%%%% Ouput:
%%%%%    tree: Calling Context Tree resulting from the prossecing of the
%%%%%    file
function [ tree_sctr ] = contextTree( File )

tline = filetoarray(File);

%%% Initialize the data structure of the tree
is_prev_return = false; %%% Flag used to know if the previous action was to call a function or to return from one
[tree_sctr] = initTree();
treeNode = tree_sctr{1};
already_child = false; %% initializes the variable that checks if a node already has the presented child
node_index = 2;

[token, remain] = strtok(tline(1:end));


for i = 1:length(tline)


    if strcmp( token(i), 'CALL') %% there is the word CALL in the line
        is_prev_return = false;
            if ~isempty(treeNode.child)
                %%% loops over the children to check if the one now already
                %%% exists

                for j = 1:length(treeNode.child)
                     
                    if strcmp(tree_sctr{treeNode.child(j)}.name, remain(i)) %% If found the call as one of the children, doesn't add again
                        already_child = true;
                        child_index = treeNode.child(j);
                    end
                end

            else
                already_child = false;
            end
            
            if already_child == false %% If didn't find the child, adds it and goes to it
                treeNode.child(end+1) = node_index;
                [treeNode, tree_sctr] = addNode(treeNode, node_index, tree_sctr, remain(i));
                node_index = node_index +1; %%% increments only if it was not a child. Which means I am adding the node
                
            else
                already_child = false;
                [treeNode, tree_sctr] = updateNode(treeNode, tree_sctr, child_index);
            end
       
    elseif strcmp( token(i), 'RETURN') %% there is the word RETURN in the line
        
        if ~isempty(treeNode.parent) %% if it is empty it is the root
            if ~is_prev_return
                [tree_sctr] = markAsStack(treeNode, tree_sctr);
                is_prev_return = true; % updates the flag to true
            end
            treeNode = tree_sctr{treeNode.parent};
        end
    end
    
end

end



%%% Initializes the tree
function [tree] = initTree ()

tree = {};
treeNode.name = 'ROOT';
treeNode.parent = [];
treeNode.child = [];
treeNode.times_called = 1;
treeNode.index = 1;
treeNode.stackbottom = false;
treeNode.stack_shows = 0; %%% Only the not that is a stack bottom should have this different than 0;
tree{1} = treeNode;

end


%%% It marks that node as being the top of one stack
function [tree] = markAsStack(treeNode, tree)
tree{treeNode.index}.stackbottom = true; % mark as the bottom of a stack
var = tree{treeNode.index}.stack_shows;
var = var +1;
tree{treeNode.index}.stack_shows = var; % increments the number of times the stack appeared
end

%%% When a node is called, this function updates its counter showing how
%%% many times that node was called in that path
function [treeNode, tree] = updateNode(treeNode, tree, child_index)

called = tree{child_index}.times_called;
called = called +1;
tree{child_index}.times_called = called;
treeNode = tree{child_index}; %% accesses the child
end


%%% Used to add children for a given node. It is not used for the first
%%% child of the root
function [treeNode, tree] = addNode(treeNode, node_index, tree, name)

tree{treeNode.index} = treeNode; %% updates the current tree node
treeNode.name = name;
treeNode.parent = treeNode.index;
treeNode.child = [];
treeNode.times_called = 1;
treeNode.index = node_index;
tree{node_index} = treeNode;

end

%%% Reads from a file and puts the entire document inside the variable
%%% tline. Each line of the document is an index of the vector
function [tline] = filetoarray(File)

fid = fopen(File);
tline = textscan(fid,'%s','Delimiter','\n');
tline = tline{1};

fclose(fid);

end