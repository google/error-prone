classdef tree
%% TREE  A class implementing a tree data structure.
%
% This class implements a simple tree data structure. Each node can only
% have one parent, and store any kind of data. The root of the tree is a
% privilieged node that has no parents and no siblings.
%
% Nodes are mainly accessed through their index. The index of a node is
% returned when it is added to the tree, and actually corresponds to the
% order of addition.
%
% Basic methods to tarverse and manipulate trees are implemented. Most of
% them take advantage of the ability to create _coordinated_ trees: If
% a tree is duplicated and only the new tree data content is modified
% (i.e., no nodes are added or deleted), then the iteration order and the
% node indices will be the same for the two trees.
%
% Internally, the class simply manage an array referencing the node parent
% indices, and a cell array containing the node data. 

% Jean-Yves Tinevez <tinevez@pasteur.fr> March 2012
    
    properties (SetAccess = private)
        % Hold the data at each node
        Node = { [] };
        
        % Index of the parent node. The root of the tree as a parent index
        % equal to 0.
        Parent = [ 0 ]; %#ok<NBRAK>
        
    end
    
    methods
        
        % CONSTRUCTOR
        
        function [obj, root_ID] = tree(content, val)
            %% TREE  Construct a new tree
            %
            % t = TREE(another_tree) is the copy-constructor for this
            % class. It returns a new tree where the node order and content
            % is duplicated from the tree argument.
            % 
            % t = TREE(another_tree, 'clear') generate a new copy of the
            % tree, but does not copy the node content. The empty array is
            % put at each node.
            %
            % t = TREE(another_tree, val) generate a new copy of the
            % tree, and set the value of each node of the new tree to be
            % 'val'.
            %
            % t = TREE(root_content) where 'root_content' is not a tree,
            % initialize a new tree with only the root node, and set its
            % content to be 'root_content'.
           
            if nargin < 1
                root_ID = 1;
                return
            end
            
            if isa(content, 'tree')
                % Copy constructor
                obj.Parent = content.Parent;
                if nargin > 1 
                    if strcmpi(val, 'clear')
                        obj.Node = cell(numel(obj.Parent), 1);
                    else
                        cellval = cell(numel(obj.Parent), 1);
                        for i = 1 : numel(obj.Parent)
                            cellval{i} = val;
                        end
                        obj.Node = cellval;
                    end
                else
                    obj.Node = content.Node;
                end
                
            else
                % New object with only root content
                
                obj.Node = { content };
                root_ID = 1;
            end
            
        end
        
        
        % METHODS
        
        function [obj, ID] = addnode(obj, parent, data)
            %% ADDNODE attach a new node to a parent node
            % 
            % tree = tree.ADDNODE(parent_index, data) create a new node
            % with content 'data', and attach it as a child of the node
            % with index 'parent_index'. Return the modified tree.
            % 
            % [ tree ID ] = tree.ADDNODE(...) returns the modified tree and
            % the index of the newly created node.
            
            if parent < 0 || parent > numel(obj.Parent)
                error('MATLAB:tree:addnode', ...
                    'Cannot add to unknown parent with index %d.\n', parent)
            end
            
            if parent == 0
                % Replace the whole tree by overiding the root.
                obj.Node = { data };
                obj.Parent = 0;
                ID = 1;
                return
            end
            
            obj.Node = [
                obj.Node
                data
                ];
            
            obj.Parent = [
                obj.Parent
                parent ];
            
            ID = numel(obj.Node);
        
        end
        
        function flag = isleaf(obj, ID)
           %% ISLEAF  Return true if given ID matches a leaf node.
           % A leaf node is a node that has no children.
           if ID < 1 || ID > numel(obj.Parent)
                error('MATLAB:tree:isleaf', ...
                    'No node with ID %d.', ID)
           end
           
           parent = obj.Parent;
           flag = ~any( parent == ID );
           
        end
        
        function IDs = findleaves(obj)
           %% FINDLEAVES  Return the IDs of all the leaves of the tree.
           parents = obj.Parent;
           IDs = (1 : numel(parents)); % All IDs
           IDs = setdiff(IDs, parents); % Remove those which are marked as parent
           
        end
        
        function content = get(obj, ID)
            %% GET  Return the content of the given node ID.
            content = obj.Node{ID};
        end

        function obj = set(obj, ID, content)
            %% SET  Set the content of given node ID and return the modifed tree.
            obj.Node{ID} = content;
        end

        
        function IDs = getchildren(obj, ID)
        %% GETCHILDREN  Return the list of ID of the children of the given node ID.
        % The list is returned as a line vector.
            parent = obj.Parent;
            IDs = find( parent == ID );
            IDs = IDs';
        end
        
        function ID = getparent(obj, ID)
        %% GETPARENT  Return the ID of the parent of the given node.
            if ID < 1 || ID > numel(obj.Parent)
                error('MATLAB:tree:getparent', ...
                    'No node with ID %d.', ID)
            end
            ID = obj.Parent(ID);
        end
        
        function IDs = getsiblings(obj, ID)
            %% GETSIBLINGS  Return the list of ID of the sliblings of the 
            % given node ID, including itself.
            % The list is returned as a column vector.
            if ID < 1 || ID > numel(obj.Parent)
                error('MATLAB:tree:getsiblings', ...
                    'No node with ID %d.', ID)
            end
            
            if ID == 1 % Special case: the root
                IDs = 1;
                return
            end
            
            parent = obj.Parent(ID);
            IDs = obj.getchildren(parent);
        end
        
        function n = nnodes(obj)
            %% NNODES  Return the number of nodes in the tree. 
            n = numel(obj.Parent);
        end
        
        
    end
    
    % STATIC METHODS
    
    methods (Static)
        
        hl = decorateplots(ha)
        
        function [lineage, duration] = example
            
            lineage_AB = tree('AB');
            [lineage_AB, id_ABa] = lineage_AB.addnode(1, 'AB.a');
            [lineage_AB, id_ABp] = lineage_AB.addnode(1, 'AB.p');
            
            [lineage_AB, id_ABal] = lineage_AB.addnode(id_ABa, 'AB.al');
            [lineage_AB, id_ABar] = lineage_AB.addnode(id_ABa, 'AB.ar');
            [lineage_AB, id_ABala] = lineage_AB.addnode(id_ABal, 'AB.ala');
            [lineage_AB, id_ABalp] = lineage_AB.addnode(id_ABal, 'AB.alp');
            [lineage_AB, id_ABara] = lineage_AB.addnode(id_ABar, 'AB.ara');
            [lineage_AB, id_ABarp] = lineage_AB.addnode(id_ABar, 'AB.arp');
            
            [lineage_AB, id_ABpl] = lineage_AB.addnode(id_ABp, 'AB.pl');
            [lineage_AB, id_ABpr] = lineage_AB.addnode(id_ABp, 'AB.pr');
            [lineage_AB, id_ABpla] = lineage_AB.addnode(id_ABpl, 'AB.pla');
            [lineage_AB, id_ABplp] = lineage_AB.addnode(id_ABpl, 'AB.plp');
            [lineage_AB, id_ABpra] = lineage_AB.addnode(id_ABpr, 'AB.pra');
            [lineage_AB, id_ABprp] = lineage_AB.addnode(id_ABpr, 'AB.prp');
            
            lineage_P1 = tree('P1');
            [lineage_P1, id_P2] = lineage_P1.addnode(1, 'P2');
            [lineage_P1, id_EMS] = lineage_P1.addnode(1, 'EMS');
            [lineage_P1, id_P3] = lineage_P1.addnode(id_P2, 'P3');
            
            [lineage_P1, id_C] = lineage_P1.addnode(id_P2, 'C');
            [lineage_P1, id_Ca] = lineage_P1.addnode(id_C, 'C.a');
            [lineage_P1, id_Caa] = lineage_P1.addnode(id_Ca, 'C.aa');
            [lineage_P1, id_Cap] = lineage_P1.addnode(id_Ca, 'C.ap');
            [lineage_P1, id_Cp] = lineage_P1.addnode(id_C, 'C.p');
            [lineage_P1, id_Cpa] = lineage_P1.addnode(id_Cp, 'C.pa');
            [lineage_P1, id_Cpp] = lineage_P1.addnode(id_Cp, 'C.pp');
            
            [lineage_P1, id_MS] = lineage_P1.addnode(id_EMS, 'MS');
            [lineage_P1, id_MSa] = lineage_P1.addnode(id_MS, 'MS.a');
            [lineage_P1, id_MSp] = lineage_P1.addnode(id_MS, 'MS.p');
            
            [lineage_P1, id_E] = lineage_P1.addnode(id_EMS, 'E');
            [lineage_P1, id_Ea] = lineage_P1.addnode(id_E, 'E.a');
            [lineage_P1, id_Eal] = lineage_P1.addnode(id_Ea, 'E.al'); %#ok<*NASGU>
            [lineage_P1, id_Ear] = lineage_P1.addnode(id_Ea, 'E.ar');
            [lineage_P1, id_Ep] = lineage_P1.addnode(id_E, 'E.p');
            [lineage_P1, id_Epl] = lineage_P1.addnode(id_Ep, 'E.pl');
            [lineage_P1, id_Epr] = lineage_P1.addnode(id_Ep, 'E.pr');
            
            [lineage_P1, id_P4] = lineage_P1.addnode(id_P3, 'P4');
            [lineage_P1, id_Z2] = lineage_P1.addnode(id_P4, 'Z2');
            [lineage_P1, id_Z3] = lineage_P1.addnode(id_P4, 'Z3');
            
            
            [lineage_P1, id_D] = lineage_P1.addnode(id_P3, 'D');
            
            lineage = tree('Zygote');
            lineage = lineage.graft(1, lineage_AB);
            lineage = lineage.graft(1, lineage_P1);

            
            duration = tree(lineage, 'clear');
            iterator = duration.depthfirstiterator;
            for i = iterator
               duration = duration.set(i, round(20*rand)); 
            end
            
        end
        
    end
    
end

