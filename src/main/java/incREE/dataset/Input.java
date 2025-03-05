package incREE.dataset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Input {
    private Scanner scanner;
    private int lineCount = 0;
    private final int columnCount;
    private final String[] header;

    public Input(String fileName) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (inputStream == null) {
                System.out.println("File not found: " + fileName);
                System.exit(1);
            } else {
                scanner = new Scanner(inputStream);
            }
        } catch (Exception e) {
            System.out.println("File not found: " + fileName);
            System.exit(1);
        }

        header = scanner.nextLine().split(",");
        columnCount = header.length;
    }

    private List<RawColumn> readLines(int count) {
        List<RawColumn> rawColumns = new ArrayList<RawColumn>();
        for (String s : header) {
            rawColumns.add(new RawColumn(s, count));
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] columns = line.split(",");
            for (int i = 0; i < columns.length; i++) {
                rawColumns.get(i).addLine(columns[i]);
            }
            lineCount++;
        }
        scanner.close();
        return rawColumns;
    }

    public Relation getRelation(int line) {
        List<RawColumn> rawColumns = readLines(line);
        List<Column<?>> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(rawColumns.get(i).build(lineCount));
        }
        if (lineCount < line) {
            throw new IndexOutOfBoundsException();
        }
        return new Relation(columns, line, lineCount);
    }
}
