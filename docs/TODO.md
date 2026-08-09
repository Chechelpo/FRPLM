Early access 0.1.0
 - Add chat history budget + lorebook editors for prompts ✓
 - Fix intent classifier to correctly assign location changes on a default prompt ✓
 - Fix movement in responses (weird thing happening) ✓
 - Before release:
   - Test a campaign ✓
   - Update frplm-build-helper matching the new extension-sdk version templates ✓
   - Publish the new extension-sdk under version 0.1.3 ✓

0.2.0
 - Add editing directly on-graph
 - Add prolog predicates
 - Add prolog goals to:
   - Edge visibility
   - Traversability
   - As entry activation step (post-keywords)

Bugs: 
 - Colapsar una region no cambia los bounds para reparentar entonces se puede reparentar una locacion o region a una regio colapsada.
 - El visor de editor de mundo tapa los errores y no cambia tamanio de forma natural cuando se expande el editor