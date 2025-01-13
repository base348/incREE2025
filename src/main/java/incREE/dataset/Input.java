package incREE.dataset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Input {
    private final int lineCountLimit;
    private Scanner scanner;
    private int lineCount = 0;
    private final int columnCount;
    private final List<RawColumn> rawColumns = new ArrayList<RawColumn>();

    public Input(String fileName, int lineCountLimit) {
        this.lineCountLimit = lineCountLimit;
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

        String[] header = scanner.nextLine().split(",");
        for (String s : header) {
            rawColumns.add(new RawColumn(s));
        }
        columnCount = rawColumns.size();
    }

    private void readLines() {
        while (scanner.hasNextLine() && lineCount < lineCountLimit) {
            String line = scanner.nextLine();
            String[] columns = line.split(",");
            for (int i = 0; i < columns.length; i++) {
                rawColumns.get(i).addLine(columns[i]);
            }
            lineCount++;
        }
    }

    public Relation toRelation() {
        readLines();
        List<Column<?>> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(rawColumns.get(i).build(lineCount));
        }
        return new Relation(columns, lineCount);
    }
}
