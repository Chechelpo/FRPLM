# Frplm
A LLM frontend framework specifically designed for long-term sessions with a focus on a world rather than individual characters. 

# Current features
These are included in the latest release:
1. Exporting/importing worlds via JSON files. You can share or backup your creations
2. Defining a hierarchical system of a world with regions within regions, each one with their own locations and characters in them. Each of these have their own description and associated lorebooks.
3. A location system that separates traversability and visibility, represented via a graph.
4. An extension system with an extensive frontend and backend SDK, with published packages in npm and maven respectively.

# Usage
Download the latest release, navigate to the directory of your choosing and run 
```
java -jar frplm-engine-<version>.jar
```
The frontend should then be available under: http://localhost:8080

# Extensions
For usage, just drop the compiled java artifact under ./plugins.

See https://github.com/Chechelpo/frplm-extension-sdk for a guide on how to develop them.

## License

This project is licensed under the PolyForm Noncommercial License 1.0.0.

You may use, copy, modify, and redistribute this software for non-commercial
purposes.

Commercial use is not permitted without a separate commercial licence from the
copyright holder.

For commercial licensing, contact: felipeyelpo26@gmail.com

