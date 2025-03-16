package incREE.dataset;

import incREE.evidence.PredicateBitmap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;

public class Input {
    private Scanner scanner;
    private BufferedReader  bufferedReader;
    private int lineCount = 0;
    private int columnCount;
    private String[] header;

    public Input(String fileName) {
//        try {
//            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
//            if (inputStream == null) {
//                System.out.println("Input failed: file not found: " + fileName);
//                System.exit(1);
//            } else {
//                scanner = new Scanner(inputStream);
//            }
//        } catch (Exception e) {
//            System.out.println("Input failed: file not found: " + fileName);
//            System.exit(1);
//        }
//
//        header = scanner.nextLine().split(",");
//        columnCount = header.length;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            header = bufferedReader.readLine().split(",");
            columnCount = header.length;
        } catch (IOException e) {
            System.out.println("Input failed: file not found: " + fileName);
            System.exit(1);
        }
    }

    private List<RawColumn> readLines(int count) {
        List<RawColumn> rawColumns = new ArrayList<RawColumn>();
        for (String s : header) {
            rawColumns.add(new RawColumn(s, count));
        }
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] columns = line.split(",");
                for (int i = 0; i < columns.length; i++) {
                    rawColumns.get(i).addLine(columns[i]);
                }
                lineCount++;
            }
        } catch (IOException e) {
            System.out.println("Input failed: line not found: " + lineCount);
        }
        bufferedReader = null;
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
