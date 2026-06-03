package chechelpo.frplm.extensions.api.results;

import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public sealed interface MoveResult {
    record SuccessfulMove(SessionCharacter moved, SessionLocation from, SessionLocation to) implements MoveResult {}
    record FailedMove(Type type, SessionCharacter moved, SessionLocation from, SessionLocation to, String message) implements MoveResult {
        public enum Type {
            ALREADY_AT_LOCATION,
            NOT_NEIGHBOURS,
            UNKNOWN
        }
        @Override
        public @NotNull String toString() {
            return "MOVEMENT ERROR: from [" + from + "] to [" + to + "] \n MESSAGE:" + message;
        }
    }

    default boolean failed(){
        return this instanceof FailedMove;
    }
    default boolean successful(){
        return this instanceof SuccessfulMove;
    }

    default SuccessfulMove getSuccess(){
        if (!successful()) throw new IllegalStateException("Move failed yet was asked for successful move");
        return (SuccessfulMove) this;
    }
    default FailedMove getFailed(){
        if (!failed()) throw new IllegalStateException("Move failed yet was asked for failed");
        return (FailedMove) this;
    }

    @Contract(value = "_,_ -> new", pure = true)
    static @NotNull MoveResult alreadyAtLocation(SessionCharacter moved, SessionLocation location){
        return new FailedMove(FailedMove.Type.ALREADY_AT_LOCATION, moved, location, location, "Character already at location");
    }

    @Contract(value = "_,_,_ -> new", pure = true)
    static @NotNull MoveResult notNeighbours(SessionCharacter moved, SessionLocation location, SessionLocation other){
        return new FailedMove(FailedMove.Type.NOT_NEIGHBOURS, moved, location, other, "These locations are not neighbours");
    }

    @Contract(value = "_,_,_ -> new", pure = true)
    static @NotNull MoveResult success(SessionCharacter moved, SessionLocation from, SessionLocation to){
        return new SuccessfulMove(moved, from, to);
    }
}
