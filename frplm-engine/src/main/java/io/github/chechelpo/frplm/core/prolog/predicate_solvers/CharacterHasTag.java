package io.github.chechelpo.frplm.core.prolog.predicate_solvers;



import java.util.List;

public class CharacterHasTag  {
    /*
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
    }*/
}
