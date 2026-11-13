package io.github.rogeriocisi.airlab.structured;

import java.time.Instant;
import java.util.List;

/**
 * A request to a medium as that medium sees it (SPEC E13, phase C): the two first names in the
 * drawn order, the deadline, and which readings are already locked.
 *
 * <p>Built for its medium alone. By construction it has no sitter code, no request code, no
 * answer of any sitter and no score (P4 matrix); a slot is a position, not a link to a person.
 *
 * @param questionnaireVersion the version the study froze, which is the form the medium fills in
 */
public record MediumAssignment(String code,
                               AssignmentState state,
                               String questionnaireVersion,
                               Instant assignedAt,
                               Instant dueAt,
                               List<Slot> slots) {

    public MediumAssignment {
        slots = List.copyOf(slots);
    }

    public boolean open() {
        return state == AssignmentState.OPEN;
    }

    public long lockedSlots() {
        return slots.stream().filter(Slot::locked).count();
    }

    public java.util.Optional<Slot> slot(int position) {
        return slots.stream().filter(slot -> slot.position() == position).findFirst();
    }

    @Override
    public String toString() {
        return "MediumAssignment[code=" + code + ", state=" + state + ", redacted]";
    }

    /**
     * One of the two readings of the pair.
     *
     * @param position  1 or 2, the drawn order
     * @param firstName the first name released to this medium
     */
    public record Slot(int position, String firstName, boolean locked, Instant lockedAt) {

        @Override
        public String toString() {
            return "Slot[position=" + position + ", locked=" + locked + ", redacted]";
        }
    }
}
