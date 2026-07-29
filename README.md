# Frplm
<img width="1912" height="859" alt="Screenshot_20260728_154028" src="https://github.com/user-attachments/assets/88998f12-5ef7-4f30-a4fb-f6a874f1fc12" />
A LLM web-app specifically designed for long-term sessions with a focus on a world rather than individual characters. 

# Current features
These are included in the latest release:
1. Exporting/importing worlds via JSON files. You can share or backup your creations
2. Defining a hierarchical system of a world with regions within regions, each one with their own locations and characters in them. Each of these have their own description and associated lorebooks.
3. A location system that separates traversability and visibility, represented via a graph.
4. An extension system with frontend and backend SDK, with published packages in npm and maven respectively.
5. Import of ST lorebooks directly, with easy migration between FRPLM lorebooks.
6. Somewhat customizable appearance via settings.


# Usage
Download the latest release, navigate to the directory of your choosing and run 
```
java -jar frplm-engine-<version>.jar
```
The frontend should then be available under: http://localhost:8080

<img width="1905" height="896" alt="image" src="https://github.com/user-attachments/assets/5d552bce-710d-4348-8970-d47c57d7420a" />

# Workflow:

1. Create a world. This world has its own lorebook, name and description.

2. Create a region and an x amount of regions beneath it (essentially a region tree). Each region has its own lorebook + description.

3. Within each region, you may add locations, which are the actual places a character can be. These locations have, once again, their own lorebook, name and description. They may also be linked to one another (regardless of parent region) . These are directed edges that may be traversable or not as well as hide particular information of the destination (ex.: show the description of a door to the room rather than the description of the room itself).

4. In each location, add characters which start there. They have their own lorebooks too.


# Extensions
For usage, just drop the compiled java artifact under ./plugins.

See https://github.com/Chechelpo/frplm-extension-sdk for a guide on how to develop them.

# Support
I post both on ko-fi and patreon.
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/N4N01T3UQQ)
[![Support me on Patreon](https://img.shields.io/badge/Support_me_on-Patreon-FF424D?logo=patreon)](https://www.patreon.com/Simulith)

## License

This project is licensed under the PolyForm Noncommercial License 1.0.0.

You may use, copy, modify, and redistribute this software for non-commercial
purposes.

Commercial use is not permitted without a separate commercial licence from the
copyright holder.

For commercial licensing, contact: felipeyelpo26@gmail.com

