export type WorldGraphControlHelpEntry = Readonly<{
    control: string;
    description: string;
}>;

export type WorldGraphControlHelpSection = Readonly<{
    title: string;
    entries: readonly WorldGraphControlHelpEntry[];
}>;

/**
 * User-facing control reference for WorldEditGraph.
 *
 * Keep this list synchronized with the actual pointer/keyboard bindings and
 * context-menu actions. WorldEditGraph renders this data directly so the help
 * popup and its regression test share one source of truth.
 */
export const WORLD_GRAPH_CONTROL_HELP_SECTIONS = [
    {
        title: "Pointer, selection & movement",
        entries: [
            {control: "Primary click node", description: "Select only that region or location. Clicking an unselected node before dragging selects it first."},
            {control: "Primary drag node", description: "Move the current selection. Moving a parent carries its descendants; locked nodes cannot move relative to their parent."},
            {control: "Primary click world", description: "Clear the current selection."},
            {control: "Ctrl + primary click", description: "Toggle the node under the pointer in or out of the current selection."},
            {control: "Shift + primary drag", description: "Draw a marquee and replace the selection with visible nodes inside it."},
            {control: "Ctrl + Shift + primary drag", description: "Add the marquee result to the existing selection."},
            {control: "Secondary click", description: "Open the context menu at the pointer. Secondary-clicking an unselected node selects that node first."},
        ],
    },
    {
        title: "Viewport",
        entries: [
            {control: "Wheel", description: "Zoom in or out around the mouse pointer."},
            {control: "Alt + primary drag", description: "Pan the graph. Unmodified primary dragging never pans."},
            {control: "+ / −", description: "Zoom in or out around the viewport centre."},
            {control: "Zoom % / Reset", description: "Reset pan and zoom to the default viewport."},
        ],
    },
    {
        title: "Resize — v8 gesture",
        entries: [
            {control: "Shift + S (hold)", description: "Begin resizing immediately from the current pointer position; no mouse press is used."},
            {control: "0 / 1 / 2+ selected", description: "No selection resizes the world; exactly one region or location resizes that node; multi-selection disables resize."},
            {control: "Move away / toward", description: "While holding Shift+S, move radially away from the target centre to grow it or toward the centre to shrink it."},
            {control: "Release Shift or S", description: "Commit a valid changed resize. Escape, window blur, world change, or selection change cancels it."},
        ],
    },
    {
        title: "Keyboard shortcuts",
        entries: [
            {control: "F", description: "Focus and select the graph search field."},
            {control: "I", description: "Open the context menu for the current selection."},
            {control: "E", description: "Edit the single selected region or location. Press E again for the entity already open in the inspector to close it; another E reopens it."},
            {control: "D", description: "Open the deletion confirmation for the current selection."},
            {control: "Space", description: "Collapse or expand selected regions. Mixed collapse states collapse by default."},
            {control: "A", description: "With one location selected, create a bidirectionally linked neighbour; with one region selected, create a location at the pointer."},
            {control: "↑ / ↓ or W / S", description: "Move focus through an open context menu or its active submenu."},
            {control: "Esc", description: "Cancel the active confirmation or resize, close open popovers/creation UI, or otherwise clear the graph interaction and close editing."},
            {control: "Enter", description: "Confirm a destructive confirmation. On a focused button it activates that button; it also submits focused search and creation forms."},
        ],
    },
    {
        title: "Search",
        entries: [
            {control: "Type in search", description: "Runs after a 250 ms debounce. Names ignore case and whitespace."},
            {control: "region:name", description: "Find regions; one match focuses it, while multiple matches preserve the viewport and select visible matches."},
            {control: "location:name / character:name", description: "Find one location by name, or select starting locations for matching characters."},
            {control: "Search / Enter", description: "Run the current query immediately instead of waiting for the debounce."},
            {control: "× / Esc in search", description: "Clear the search query and its search-owned state."},
        ],
    },
    {
        title: "Top-left toolbar & messages",
        entries: [
            {control: "Back", description: "Leave the current world editor."},
            {control: "World name", description: "Open world information editing."},
            {control: "Export", description: "Export the current world."},
            {control: "Retry", description: "Retry a persistent graph-load failure when the status message offers it."},
            {control: "Message ×", description: "Dismiss a non-persistent status message immediately."},
            {control: "?", description: "Open or close this complete world-graph control reference."},
        ],
    },
    {
        title: "Context menu actions",
        entries: [
            {control: "World", description: "Create root region; set/remove background; edit world."},
            {control: "One region", description: "Edit; lock/unlock; collapse/expand; create location; create sub-region; set/remove background; delete."},
            {control: "One location", description: "Edit; lock/unlock; reset to automatic size; delete."},
            {control: "Two locations", description: "Lock/unlock; create middle location; connect either direction or both; disconnect either direction or both; edit connection; link locations; delete."},
            {control: "3+ locations", description: "Lock/unlock; batch-link locations; delete selected locations."},
            {control: "2+ regions", description: "Lock/unlock; collapse/expand selected regions; delete selected regions."},
            {control: "Mixed selection", description: "Lock/unlock and delete selected nodes; location-pair or batch-link actions also appear when enough locations are selected."},
            {control: "Link locations ›", description: "Choose nearest-network (MST) or all-pairs topology, one-way or bidirectional direction, and traversability; then create only missing connections."},
            {control: "Close submenu", description: "Close the batch-link submenu without changing graph data. Escape closes all active context menus."},
            {control: "Clear selection", description: "Remove all region and location selections without changing graph data."},
        ],
    },
    {
        title: "Viewer config — bottom-right gear",
        entries: [
            {control: "Gear", description: "Open or close the per-world Viewer Config panel."},
            {control: "LOD detail", description: "Control how aggressively tiny nodes, labels, and edges are culled."},
            {control: "Viewer group headers", description: "Expand or collapse each Viewer Config group to reach its numeric fields."},
            {control: "Detail culling", description: "Node render cutoff; Label cutoff."},
            {control: "Nodes & sizing", description: "New region width/height; Region minimum width/height; New location radius; Location minimum radius; Region/world content padding; Collapsed region width/height."},
            {control: "Labels", description: "Location label/radius; Region label/width; Region label/height; Collapsed label/height."},
            {control: "Interaction", description: "Location hit radius; Region hit size; Resize doubling distance; Drag threshold; Reparent resistance."},
            {control: "Edges", description: "Stroke/node radius; Selection halo; Selected edge emphasis; Edge render cutoff; Arrowhead size; Bidirectional lane offset."},
            {control: "Reset defaults", description: "Restore every Viewer Config value to its compile-time default. Settings otherwise persist per world."},
        ],
    },
    {
        title: "Creation & confirmation UI",
        entries: [
            {control: "Create / Cancel", description: "Submit or abandon the active named region/location creation prompt; Enter submits while the name field is focused."},
            {control: "Confirm / Cancel", description: "Apply or abandon a destructive operation. Enter confirms and Escape cancels."},
            {control: "Set background", description: "Open the image picker for the world or selected region from its context menu."},
        ],
    },
] as const satisfies readonly WorldGraphControlHelpSection[];
