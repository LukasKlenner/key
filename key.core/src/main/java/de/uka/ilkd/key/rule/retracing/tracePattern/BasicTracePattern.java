package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.advanceTrace.BasicAdvanceTraceInformation;
import org.jspecify.annotations.NullMarked;
import org.key_project.util.collection.ImmutableList;

import java.util.List;

@NullMarked
public class BasicTracePattern implements TracePattern {

    private static final List<Character> VALID_PATTERN_CHARS = List.of('I', 'O', 'C', 'T', 'J', 'U', 'R', 'D', 'E', 'S');

    private final String pattern;
    private final int length;


    public BasicTracePattern(String pattern) {
        this.pattern = pattern;
        length = pattern.replace("S", "IIIIII").length();

        for (char c : pattern.toCharArray()) {
            if (!VALID_PATTERN_CHARS.contains(c)) {
                throw new IllegalArgumentException("Invalid trace pattern character: " + c + " in pattern " + pattern);
            }
        }
    }

    @Override
    public boolean matches(ImmutableList<TraceElement> elements) {
        if (elements.size() < pattern.length()) {
            return false;
        }

        for (char c : pattern.toCharArray()) {

            // handle special switch pattern
            if (c == 'S') {
                if (elements.size() < TraceElement.SWITCH_ELEMENT_COUNT) {
                    return false;
                }

                for (TraceElement elem : elements.take(TraceElement.SWITCH_ELEMENT_COUNT)) {
                    if (!elem.isIfOrElse()) {
                        return false;
                    }
                }

                elements = elements.skip(TraceElement.SWITCH_ELEMENT_COUNT);
                continue;
            }

            TraceElement elem = elements.head();
            switch (c) {
                case 'I' -> {
                    if (!(elem instanceof TraceElement.If)) return false;
                }
                case 'O' -> {
                    if (!(elem instanceof TraceElement.Else)) return false;
                }
                case 'C' -> {
                    if (!(elem instanceof TraceElement.Call)) return false;
                }
                case 'T' -> {
                    if (!(elem instanceof TraceElement.Try)) return false;
                }
                case 'J' -> {
                    if (!(elem instanceof TraceElement.Catch)) return false;
                }
                case 'U' -> {
                    if (!(elem instanceof TraceElement.TryEnd)) return false;
                }
                case 'R' -> {
                    if (!(elem instanceof TraceElement.Return)) return false;
                }
                case 'D' -> {
                    if (!(elem instanceof TraceElement.Dim)) return false;
                }
                case 'E' -> {
                    if (!(elem instanceof TraceElement.End)) return false;
                }
                default -> throw new IllegalStateException(
                        "Invalid trace pattern character: " + c + " in pattern " + pattern);
            }
            elements = elements.tail();
        }

        return true;
    }

    @Override
    public AdvanceTraceInformation getDefaultAdvanceTraceInformation() {
        return new BasicAdvanceTraceInformation(length);
    }
}
