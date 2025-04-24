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
    private int length = 0;
    private int lengthPLI = 0;
    private int columnCount;
    private String[] header;
    List<RawColumn> rawColumns = new ArrayList<RawColumn>();
    private String filename;

    public Input() {}

    public Input(String fileName) {
        openCSV(fileName);
    }

    public Input openCSV(String fileName) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            bufferedReader = new BufferedReader(new FileReader(fileName));
            if (header == null) {
                header = bufferedReader.readLine().split(",");
                columnCount = header.length;
                for (String s : header) {
                    rawColumns.add(new RawColumn(s));
                }
            } else {
                // check valid inc file
                String[] newHeader = bufferedReader.readLine().split(",");
                if (header.length != newHeader.length) {
                    System.err.println("Input failed: Columns of " + fileName + " and " + this.filename + " cannot match!");
                    System.exit(-1);
                }
                for (int i = 0; i < header.length; i++) {
                    if (!header[i].equals(newHeader[i])) {
                        System.err.println("Input failed: Columns of " + fileName + " and " + this.filename + " cannot match!");
                        System.exit(-1);
                    }
                }
            }
            this.filename = fileName;
            System.out.println("File " + this.filename + " opened successfully.");
        } catch (IOException e) {
            System.err.println("Input failed: file not found: " + fileName);
            System.exit(-1);
        }
        return this;
    }

    public Input read(int lineCount) {
        if (lineCount == 0) {
            return this;
        }
        if (bufferedReader == null) {
            System.err.println("Input failed: No file opened.");
            System.exit(-1);
        }
        if (lengthPLI < length) {
            System.err.println("Input failed: Already have inc data.");
            System.exit(-1);
        }

        int newLines = parse(lineCount);
        if (newLines > 0) {
            System.out.println("Successfully read " + newLines + " lines.");
        }
        lengthPLI += newLines;
        length  += newLines;
        return this;
    }
    public Input read() {
        return read(-1);
    }

    public Input readInc(int lineCount) {
        if (bufferedReader == null) {
            System.err.println("Input failed: No file opened.");
            System.exit(-1);
        }

        int newLines = parse(lineCount);
        if (newLines > 0) {
            System.out.println("Successfully read " + newLines + " increment lines.");
        }
        length += newLines;
        return this;
    }

    private int parse(int maxLine) {
        String line = null;
        String[] columns;
        String regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        int lineCount = 0;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                columns = line.split(regex);
                for (int i = 0; i < columns.length; i++) {
                    rawColumns.get(i).addLine(columns[i]);
                }
                lineCount++;
                if (lineCount == maxLine) {
                    return lineCount;
                }
            }
            bufferedReader.close();
            bufferedReader = null;
            System.out.println("Reached end of file " + filename + ". File is closed.");
        } catch (IOException e) {
            System.out.println("Input failed: line not found: " + length);
            System.exit(-1);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Input failed: line with wrong number of columns: " + length);
            System.out.println(line);
            System.exit(-1);
        }
        return lineCount;
    }


    public Relation getRelation() {
        List<Column<?>> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(rawColumns.get(i).build(length, lengthPLI));
        }
        return new Relation(filename.substring(0, filename.length()-4), columns, lengthPLI);
    }

}
