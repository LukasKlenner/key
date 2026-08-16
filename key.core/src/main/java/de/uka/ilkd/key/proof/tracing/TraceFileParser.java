package de.uka.ilkd.key.proof.tracing;

import org.key_project.util.collection.ImmutableList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TraceFileParser {

    public static ImmutableList<TraceElement> parse(Path traceFile) throws IOException {
        String content = Files.readString(traceFile, StandardCharsets.UTF_8).trim();
        return parse(content);
    }

    public static ImmutableList<TraceElement> parse(String traceString) {
        List<TraceElement> elements = new ArrayList<>();
        int i = 0;
        while (i < traceString.length()) {
            char c = traceString.charAt(i);
            switch (c) {
                case 'C' -> {
                    i++;
                    int start = i;
                    while (i < traceString.length() && isHexDigit(traceString.charAt(i))) {
                        i++;
                    }
                    int functionId = Integer.parseInt(traceString.substring(start, i), 16);
                    elements.add(new TraceElement.Call(functionId));
                }
                case 'I' -> {
                    elements.add(new TraceElement.If());
                    i++;
                }
                case 'O' -> {
                    elements.add(new TraceElement.Else());
                    i++;
                }
                case 'R' -> {
                    // ignored by key
                    i++;
                }
                case 'T' -> {
                    elements.add(new TraceElement.Try());
                    i++;
                }
                case 'U' -> {
                    elements.add(new TraceElement.TryEnd());
                    i++;
                }
                case 'J' -> {
                    i++;
                    int start = i;
                    while (i < traceString.length() && isHexDigit(traceString.charAt(i))) {
                        i++;
                    }
                    int catchIndex = Integer.parseInt(traceString.substring(start, i), 16);
                    elements.add(new TraceElement.Catch(catchIndex));
                }
                case 'E' -> {
                    elements.add(new TraceElement.End());
                    i++;
                }
                default -> throw new IllegalArgumentException(
                        "Unknown trace character '" + c + "' at position " + i);
            }
        }
        return ImmutableList.fromList(elements);
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
