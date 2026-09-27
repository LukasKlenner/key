package de.uka.ilkd.key.proof.retracing;

public sealed interface TraceElement {

    int SWITCH_ELEMENT_COUNT = 6;

    default boolean isIfOrElse() {
        return this instanceof If || this instanceof Else;
    }

    record Call(int functionID) implements TraceElement {

    }

    record Catch(int catchIndex) implements TraceElement {

    }

    record If() implements TraceElement {

    }

    record Else() implements TraceElement {

    }

    record Return() implements TraceElement {

    }

    record Try() implements TraceElement {

    }

    record TryEnd() implements TraceElement {

    }

    record End() implements TraceElement {

    }

}
