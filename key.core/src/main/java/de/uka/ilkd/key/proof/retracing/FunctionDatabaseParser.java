package de.uka.ilkd.key.proof.retracing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionDatabaseParser {

    public static Map<Integer, String> parse(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        Map<Integer, String> functionMap = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int commaIndex = line.indexOf(',');
            if (commaIndex < 0) {
                throw new IllegalArgumentException(
                        "Invalid function database line (no comma): " + line);
            }
            int id = Integer.parseInt(line.substring(0, commaIndex).trim());
            String signature = line.substring(commaIndex + 1).trim();
            functionMap.put(id, signature);
        }
        return functionMap;
    }
}
