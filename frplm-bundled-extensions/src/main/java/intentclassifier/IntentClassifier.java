package intentclassifier;

import chechelpo.frplm.extensions.api.activation.PostGenerationActivated;
import chechelpo.frplm.extensions.api.annotations.FrplmExtension;
import chechelpo.frplm.extensions.api.results.MoveResult;
import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
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

@FrplmExtension
public class IntentClassifier extends ConfigurableExtension implements PostGenerationActivated {
    private static final ObjectMapper mapper = new ObjectMapper();

    public IntentClassifier() {
        super("intentclassifier",
                "Intent classifier",
                "Automatically picks up movements from chat history",
                null,
                loadResourceText(
                        IntentClassifier.class,
                        "/intentclassifier/config-panel/"
                ),
                getDefault()
        );
        setFieldConfig("connection", new FieldConfig("Connection",
                null,
                new Field.SnapshotSelection<>(true, 1, 1, ConnectionSnapshot.class))
        );
        setFieldConfig("chatHistoryToInclude", new FieldConfig(
                "Message context",
                "How many messages back to give the classifier as context",
                new Field.PrimitiveField.IntegerConfig(false, 0, Integer.MAX_VALUE))
        );
    }

    private static ObjectNode getDefault() {
        ObjectNode root = mapper.createObjectNode();
        root.putArray("connection");
        root.put("chatHistoryToInclude", 3);
        return root;
    }

    @Override
    public @NotNull String configPanelUrl() {
        return super.configPanelUrl();
    }

    @Override
    public void onNewGeneration(@NotNull Session session) {
        Config config = toConfig();

        List<ChatMessage> chatHistory = session.getLastMessages(config.chatHistory);
        ConnectionSnapshot connection;
        if (config.connectionRef.isPresent())
            connection = this.getRepository().getConnection(config.connectionRef.get()).orElseThrow();
        else connection =
                session.getPrompt()
                        .orElseThrow(() -> new IllegalStateException("Somehow the session with a new message has no prompt"))
                        .getAssignedConnection()
                        .orElseThrow(() -> new IllegalStateException("Somehow the session with a new message has no connection"));

        SessionLocation currentLocation = session.getUserCharacter().getCurrentLocation();
        SessionCharacter[] present = currentLocation.getCharactersHere();
        SessionLocation[] neighbours = currentLocation.getSessionNeighbours();


        String[] characterNames = Arrays.stream(present).map(SessionCharacter::getName).toArray(String[]::new);
        String[] neighbourNames = Arrays.stream(neighbours).map(SessionLocation::getName).toArray(String[]::new);

        this.logger().info("""
                present: %s,
                currentLocation: %s,
                neighbours: %s,
                chatHistory: %s
                """.formatted(
                characterNames,
                currentLocation.getName(),
                neighbourNames,
                chatHistory.stream().map(mess -> mess.asChatCompletion().content()).toList()
        ));
        ChatCompletionResponse response = connection.generate(
                ChatCompletionRequest.builder(connection.getModelID())
                        .appendAsSystem("""
                                You are an intent classifier.
                                
                                Determine whether any present character clearly intends to move from the current location
                                to one of the neighbouring locations.
                                
                                Return only structured JSON matching the provided schema.
                                
                                Rules:
                                - Return only characters who clearly intend to move.
                                - If no character clearly intends to move, return {"characters": []}.
                                - "character_name" must be one of the present character names.
                                - "location_name" must be one of the neighbouring location names.
                                - Do not invent characters.
                                - Do not invent locations.
                                - Do not include characters who are staying, hesitating, only discussing movement hypothetically, or not clearly moving.
                                """)
                        .appendAsUser("""
                                Present characters: %s
                                User character is: %s
                                Current location: %s
                                Neighbouring locations: %s
                                """.formatted(
                                characterNames,
                                session.getUserCharacter().getName(),
                                currentLocation.getName(),
                                neighbourNames
                        ))
                        .appendAll(chatHistory.stream()
                                .map(ChatMessage::asChatCompletion)
                                .toList())
                        .responseFormat(movementIntentFormat(characterNames, neighbourNames))
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

        for (CharacterMovementIntent charMovement : result.characters) {
            Optional<SessionCharacter> movedCharacter = Arrays.stream(present)
                    .filter(character -> character.getName().equalsIgnoreCase(charMovement.characterName))
                    .findFirst();
            if (movedCharacter.isEmpty()) continue;

            String locationName = charMovement.locationName;
            for (SessionLocation location : neighbours) {
                this.logger().fine("Moving character " + movedCharacter.get().getName() + " to " + locationName);
                if (!location.getName().equalsIgnoreCase(locationName)) continue;
                MoveResult result1 = movedCharacter.get().moveTo(location);

                if (result1.successful())
                    this.logger().info("Moved character " + movedCharacter.get().getName() + " to " + location.getName());
                else this.logger().warning("Couldn't move character " + result1.getFailed());

                break;
            }
        }
    }


    @Override
    public void onConfigChange(@NotNull JsonNode newConfig) {}

    private record MovementIntentResult(@NotNull List<CharacterMovementIntent> characters) {}

    private record CharacterMovementIntent(
            @JsonProperty("character_name")
            @NotNull String characterName,

            @JsonProperty("location_name")
            String locationName
    ) {}

    private static @NotNull MovementIntentResult parseMovementIntent(@NotNull ChatCompletionResponse response) {
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

    private record Config(Optional<ConnectionSnapshot.Reference> connectionRef, int chatHistory) {
    }

    private @NotNull Config toConfig() {
        JsonNode node = this.getCurrentConfig();

        Optional<ConnectionSnapshot.Reference> connectionRef = Optional.empty();

        JsonNode connectionNode = node.get("connection");
        if (connectionNode != null && connectionNode.isArray() && !connectionNode.isEmpty()) {
            String rawConnection = connectionNode.get(0).asString();
            if (!rawConnection.isBlank()) {
                connectionRef = Optional.of(ConnectionSnapshot.Reference.fromString(rawConnection));
            }
        }

        int chatHistoryToInclude = intField(node, "chatHistoryToInclude", 3);

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


    private static @NotNull ResponseFormat movementIntentFormat(String[] characterNames, String[] locationNames) {
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

        ObjectNode charactersNameNode = itemProperties.putObject("character_name");
        charactersNameNode.put("type", "string");
        charactersNameNode.put("description", "Name of the character being classified.");
        var characterEnum = charactersNameNode.putArray("enum");
        Arrays.stream(characterNames).forEach(characterEnum::add);


        ObjectNode locationName = itemProperties.putObject("location_name");
        locationName.put("type", "string");
        locationName.put(
                "description",
                "Destination location name."
        );
        var locationEnum = locationName.putArray("enum");
        Arrays.stream(locationNames).forEach(locationEnum::add);

        item.putArray("required")
                .add("character_name")
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
