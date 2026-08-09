package io.github.chechelpo.frplm.core.prolog.predicate_solvers;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.domain.character.tags.CharacterTagsService;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.TagSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import it.unibo.tuprolog.core.Atom;
import it.unibo.tuprolog.core.Term;
import it.unibo.tuprolog.solve.primitive.Primitive;
import kotlin.sequences.SequencesKt;

import java.util.List;

public class CharacterHasTag implements PredicateSolver {
    private CharacterTagsService characterTags;

    public CharacterHasTag(ExtensionContext context) {
        this.characterTags = context.characterTags();
    }

    @Override
    public ReservedPredicates getType() {
        return ReservedPredicates.CHARACTER_HAS_TAG;
    }

    @Override
    public Primitive getPrimitive() {
        Primitive primitive = Primitive.of(request -> {
            List<Term> arguments = request.getArguments();

            Atom characterAtom = arguments.getFirst().asAtom();
            Atom tagAtom = arguments.get(1).asAtom();

            if (characterAtom == null || tagAtom == null)
                return SequencesKt.sequenceOf(request.replyWith(false, null));

            CharacterSnapshot.Reference character =
                    CharacterSnapshot.Reference.fromString(
                            characterAtom.getValue()
                    );

            TagSnapshot.Reference tag =
                    TagSnapshot.Reference.fromString(
                            tagAtom.getValue()
                    );

            boolean hasTag = characterTags.characterHasTag(
                    character.id(),
                    tag.tagId()
            );

            return SequencesKt.sequenceOf(
                    request.replyWith(hasTag, null)
            );
        });

        return Primitive.enforcingSignature(
                getType().asSignature(),
                primitive
        );
    }
}
