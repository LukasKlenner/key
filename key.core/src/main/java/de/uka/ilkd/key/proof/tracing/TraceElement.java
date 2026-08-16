package de.uka.ilkd.key.proof.tracing;

public sealed interface TraceElement {



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
