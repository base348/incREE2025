package incREE.dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Input {
    private Scanner scanner;
    private BufferedReader  bufferedReader;
    private int lineCount = 0;
    private int columnCount;
    private String[] header;
    private final String filename;

    public Input(String fileName) {
        this.filename = fileName;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            header = bufferedReader.readLine().split(",");
            columnCount = header.length;
        } catch (IOException e) {
            System.out.println("Input failed: file not found: " + fileName);
            System.exit(1);
        }
    }

    private List<RawColumn> readLines(int read, int pli) {
        List<RawColumn> rawColumns = new ArrayList<RawColumn>();
        for (String s : header) {
            rawColumns.add(new RawColumn(s, pli));
        }
        String line = null;
        String[] columns = null;
        String regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        try {
            while ((line = bufferedReader.readLine()) != null) {
                columns = line.split(regex);
                for (int i = 0; i < columns.length; i++) {
                    rawColumns.get(i).addLine(columns[i]);
                }
                lineCount++;
                if (lineCount == read) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Input failed: line not found: " + lineCount);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Input failed: line with wrong number of columns: " + lineCount);
            System.out.println(line);
        }
        bufferedReader = null;
        return rawColumns;
    }

    public Relation getRelation(int length) {
        return getRelation(length, 0);
    }

    public Relation getRelation(int originalLength, int incLength) {
        List<RawColumn> rawColumns = readLines(originalLength+incLength, originalLength);
        List<Column<?>> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(rawColumns.get(i).build(lineCount));
        }
//        if (lineCount < length) {
//            throw new IndexOutOfBoundsException();
//        }
        return new Relation(filename.substring(0, filename.length()-4), columns, originalLength, lineCount);
    }
}
