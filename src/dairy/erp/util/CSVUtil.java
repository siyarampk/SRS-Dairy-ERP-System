package dairy.erp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple CSV reader/writer using standard Java file handling. Supports quoted
 * fields containing commas and basic escaping.
 */
public final class CSVUtil {

    private CSVUtil() {
    }

    /** Reads a CSV file into rows of string columns. The header row is preserved. */
    public static List<List<String>> read(Path path) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows.add(parseLine(line));
            }
        }
        return rows;
    }

    /** Writes rows to a CSV file (UTF-8, LF line endings). */
    public static void write(Path path, List<List<String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (List<String> row : rows) {
                writer.write(joinLine(row));
                writer.newLine();
            }
        }
    }

    /** Parses a single CSV line. Handles double-quoted fields with embedded commas. */
    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private static String joinLine(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = fields.get(i) == null ? "" : fields.get(i);
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                sb.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }
}
