function hl = decorateplots(ax)
%%DECORATEPLOTS Static utility in charge of drawing the Y tick lines.

    %% CONSTANTS DEFINITION
    
    TAG = 'TufteLine';
   
    
    %% CODE

    % Get axes children
    hchildren = get(ax, 'Children');
    
    % Do we have already our lines?
    tags = get(hchildren, 'Tag');
    tufteLines = strcmp(tags, TAG);
    
    % Draw new lines
    xl = xlim;
    ticks = get(ax, 'YTick');
    
    bgColor = get(ax, 'Color');
    hl = NaN(numel(ticks) -1, 1);
    if strcmpi(bgColor, 'none')

        % Gray, dashed ticks
        bgColor = [0.7 0.7 0.7]; % If we put our axes transparent
        index = 1;
        for t = ticks(1 : end - 1)
            hl(index) = line( xl + xl(2)/1e2, [t t], ...
                'LineWidth', 0.5, ...
                'LineStyle', '--', ...
                'Color', bgColor,...
                'Tag', TAG);
            index = index + 1;
        end
        
        % Now put them at the currect layer: they have to be below
        % everything
        uistack(hl, 'bottom');
        
    else
        
        % White ticks
        index = 1;
        for t = ticks(1 : end - 1)
            hl(index) = line( xl + xl(2)/1e2, [t t], ...
                'LineWidth', 2, ...
                'Color', bgColor,...
                'Tag', TAG);
            index = index + 1;
        end
        
        % Now put them at the currect layer: they have to be above all lines
        % objects, but below all text objects
        types = get(hchildren, 'Type');
        uistack(hl, 'top');
        textHandles = strcmp(types, 'text');
        uistack(hchildren(textHandles), 'top');
    end
    
    % Remove old lines
    delete(hchildren(tufteLines));
    
   

end