package intentclassifier;

import chechelpo.frplm.extensions.api.activation.PostGenerationActivated;
import chechelpo.frplm.extensions.api.annotations.FrplmExtension;
import chechelpo.frplm.extensions.api.results.MoveResult;
import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.extensions.api.types.ConfigurableExtension;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.openai_compatible.ResponseFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@FrplmExtension
public class EntryPoint extends ConfigurableExtension implements PostGenerationActivated {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ResponseFormat responseFormat = movementIntentFormat();
    public EntryPoint() {
        super("intent-classifier",
                "Intent classifier",
                "Automatically picks up movements from chat history",
                null,
                getDefault()
        );
    }

    @Override
    public void onNewGeneration(@NotNull Session session) {
        Config config = toConfig();

        List<ChatMessage> chatHistory = getLast(config.chatHistory, session.getChatHistory());
        ConnectionSnapshot connection;
        if (config.connectionRef.isPresent()) connection = this.getRepository().getConnection(config.connectionRef.get()).orElseThrow();
        else connection =
                session.getPrompt()
                .orElseThrow(() -> new IllegalStateException("Somehow the session with a new message has no prompt"))
                .getAssignedConnection()
                .orElseThrow(() -> new IllegalStateException("Somehow the session with a new message has no connection"));

        SessionLocation currentLocation = session.getUserCharacter().getCurrentLocation();
        SessionCharacter[] present = currentLocation.getCharactersHere();
        SessionLocation[] neighbours = currentLocation.getSessionNeighbours();

        this.logger().info("""
                present: %s,
                currentLocation: %s,
                neighbours: %s,
                chatHistory: %s
                """.formatted(
                Arrays.stream(present).map(SessionCharacter::getName).toList(),
                currentLocation.getName(),
                java.util.Arrays.stream(neighbours)
                        .map(LocationSnapshot::getName)
                        .toList(),
                chatHistory.stream().map(ChatMessage::asChatCompletion).toList()
        ));
        ChatCompletionResponse response = connection.generate(
                ChatCompletionRequest.builder(connection.getModelID())
                        .system("""
                        You are an intent classifier.

                        Determine whether the characters intend to move from the current location to one of the neighbouring locations. 
                        Including characters who haven't moved is unnecessary.

                        Return only structured JSON matching the provided schema.

                        Rules:
                        - "characters" and "characterName" must appear regardless of the number of characters.
                        - "move" is true only if the chat clearly indicates movement intent.
                        - "location_name" must be one of the neighbouring location names when move is true.
                        - Do not invent locations.
                        """)
                        .user("""
                        Present characters: %s
                        User character is: %s
                        Current location: %s
                        Neighbouring locations: %s
                        """.formatted(
                                Arrays.stream(present).map(SessionCharacter::getName).toList(),
                                session.getUserCharacter().getName(),
                                currentLocation.getName(),
                                java.util.Arrays.stream(neighbours)
                                        .map(LocationSnapshot::getName)
                                        .toList()
                        ))
                        .addAll(chatHistory.stream()
                                .map(ChatMessage::asChatCompletion)
                                .toList())
                        .responseFormat(responseFormat)
                        .build()
        );
        this.logger().setLevel(Level.FINEST);
        MovementIntentResult result = parseMovementIntent(response);
        this.logger().info("""
                result: %s
                orders: %s
                """.formatted(
                result,
                result.characters.toString()
                )
        );

        for (CharacterMovementIntent charMovement : result.characters){
            if (!charMovement.move) continue;
            Optional<SessionCharacter> movedCharacter = Arrays.stream(present)
                    .filter(character -> character.getName().equalsIgnoreCase(charMovement.characterName))
                    .findFirst();
            if (movedCharacter.isEmpty()) continue;

            String locationName = charMovement.locationName;
            for (SessionLocation location : neighbours) {
                this.logger().fine("Moving character " + movedCharacter.get().getName() + " to " + locationName);
                if (!location.getName().equalsIgnoreCase(locationName)) continue;
                MoveResult result1 = movedCharacter.get().moveTo(location);

                if (result1.successful()) this.logger().info("Moved character " + locationName + " to " + location.getName());
                else this.logger().warning("Couldn't move character " + result1.getFailed());

                break;
            }
        }
    }


    @Override
    public void onConfigChange(@NotNull JsonNode newConfig) {
        String rawConnection = textField(newConfig, "connection", "");
        int chatHistoryToInclude = intField(newConfig, "chatHistoryToInclude", 3);

        if (chatHistoryToInclude < 0) {
            throw new IllegalArgumentException("chatHistoryToInclude must be >= 0");
        }

        if (!rawConnection.isBlank()) {
            ConnectionSnapshot.Reference.fromString(rawConnection);
        }
    }

    private record MovementIntentResult(
            @NotNull List<CharacterMovementIntent> characters
    ) {}

    private record CharacterMovementIntent(
            @JsonProperty("character_name")
            @NotNull String characterName,

            boolean move,

            @JsonProperty("location_name")
            String locationName
    ) {}

    private static @NotNull MovementIntentResult parseMovementIntent(
            @NotNull ChatCompletionResponse response
    ) {
        String content = response.choices()
                .getFirst()
                .message()
                .content();

        if (content.isBlank()) {
            throw new IllegalStateException("Classifier returned an empty response");
        }

        try {
            return mapper.readValue(content, MovementIntentResult.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Classifier returned invalid movement-intent JSON: " + content,
                    e
            );
        }
    }

    private record Config(Optional<ConnectionSnapshot.Reference> connectionRef, int chatHistory){}

    private @NotNull Config toConfig() {
        JsonNode node = this.getCurrentConfig();

        String rawConnection = textField(node, "connection", "");
        int chatHistoryToInclude = intField(node, "chatHistoryToInclude", 3);

        Optional<ConnectionSnapshot.Reference> connectionRef =
                rawConnection.isBlank()
                        ? Optional.empty()
                        : Optional.of(ConnectionSnapshot.Reference.fromString(rawConnection));

        return new Config(
                connectionRef,
                Math.max(0, chatHistoryToInclude)
        );
    }
    private static String textField(@NotNull JsonNode node, @NotNull String field, @NotNull String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }
    private static int intField(@NotNull JsonNode node, @NotNull String field, int fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asInt(fallback);
    }

    private static ObjectNode getDefault() {
        return mapper.createObjectNode()
                .put("connection", "")
                .put("chatHistoryToInclude", 3);
    }

    private static @NotNull ResponseFormat movementIntentFormat() {
        ObjectNode schema = mapper.createObjectNode();

        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");

        ObjectNode characters = properties.putObject("characters");
        characters.put("type", "array");

        ObjectNode item = characters.putObject("items");
        item.put("type", "object");
        item.put("additionalProperties", false);

        ObjectNode itemProperties = item.putObject("properties");

        itemProperties.putObject("character_name")
                .put("type", "string")
                .put("description", "Name of the character being classified.")
                .put();
        itemProperties.putObject("move")
                .put("type", "boolean")
                .put("description", "Whether the character intends to move.");

        ObjectNode locationName = itemProperties.putObject("location_name");
        locationName.putArray("type")
                .add("string")
                .add("null");
        locationName.put(
                "description",
                "Destination location name when move is true; null when no movement is intended."
        );

        item.putArray("required")
                .add("character_name")
                .add("move")
                .add("location_name");

        schema.putArray("required")
                .add("characters");

        return ResponseFormat.JsonSchema(
                "character_movement_intent",
                schema,
                true
        );
    }
}
