package tools;

import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvReader {

    public static List<Map<String, String>> readCsvWithHeader(InputStream csv) throws IOException {
        return readCsvWithHeader(csv, ';', Charset.forName("UTF-8"));
    }

    public static List<Map<String, String>> readCsvWithHeader(InputStream csv, char separator) throws IOException {
        return readCsvWithHeader(csv, separator, Charset.forName("UTF-8"));
    }

    private static List<Map<String, String>> readCsvWithHeader(InputStream csv, char separator, Charset charset) throws IOException {
        int lineNumber = 1;
        try (CSVReader reader = new CSVReader(new InputStreamReader(csv, charset), separator)) {
            List<Map<String, String>> lines = new ArrayList<>();
            String[] columnNames = reader.readNext();

            CsvReader.readColumnNames(columnNames);

            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNumber++;

                if (line.length == 1 && StringUtils.isEmpty(line[0])) {
                    System.out.println(String.format("Skipping empty line %s", lineNumber));
                } else if (line.length != columnNames.length) {
                    System.out.println(String.format("Failed to parse line %s, found %s values, expecting %s", lineNumber, line.length, columnNames.length));
                } else {
                    boolean rejectingLine = false;
                    Map<String, String> map = new HashMap<>();
                    for (int ci = 0; ci < line.length; ++ci) {
                        try {
                            map.put(columnNames[ci], line[ci]);
                        } catch (Exception e) {
                            System.out.println(String.format("Error at line %s : ", lineNumber) + e);
                            rejectingLine = true;
                        }
                    }
                    if (!rejectingLine) {
                        lines.add(map);
                    }
                }
            }
            return lines;
        }
    }

    public static void readColumnNames(String[] columnNames) {
        for (int i = 0, k = columnNames.length; i < k; i++) {
            if (convertStringToHex(columnNames[i]).substring(0, 4).equals("feff")) {
                String tempHex = convertStringToHex(columnNames[i]).substring(4);
                columnNames[i] = convertHexToString(tempHex);
            }
            columnNames[i] = columnNames[i].trim();
        }
    }

    private static String convertStringToHex(String str) {

        char[] chars = str.toCharArray();

        StringBuilder hex = new StringBuilder();
        for (char aChar : chars) {
            hex.append(Integer.toHexString((int) aChar));
        }

        return hex.toString();
    }

    private static String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

        }
        return sb.toString();
    }
}
