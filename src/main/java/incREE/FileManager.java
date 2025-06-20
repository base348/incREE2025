package incREE;

import com.google.gson.reflect.TypeToken;
import incREE.cover.Cover;
import incREE.dataset.ColumnPair;
import incREE.dataset.Relation;
import incREE.evidence.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileManager {
    static Gson gson = new GsonBuilder()
            .registerTypeAdapter(PredicateBitmap.class, new PredicateBitmapAdapter())
            .create();
    static final String FILENAME = "flights";

    private static void makePath() {
        File targetDir = new File("./output/" + FILENAME);
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs();
        }
    }

    private static void makePath(String path) {
        File targetDir = new File(path);
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs();
        }
    }

    static String evidenceFileName(int lineNumber) {
        makePath("./output/" + FILENAME + "/evidence");
        return "./output/" + FILENAME + "/evidence/e_" + lineNumber + ".csv";
    }

    static String uncoveredEvidenceFileName(int lineNumber, int dcLength, int threshold) {
        makePath(String.format("./output/%s/uncovered", FILENAME));
        return String.format("./output/%s/uncovered/e_%d_%dl_%dth.csv", FILENAME, lineNumber, dcLength, threshold);
    }

    static String coverFileName(int lineNumber, int dcLength, int threshold) {
        makePath(String.format("./output/%s/cover", FILENAME));
        return String.format("./output/%s/cover/c_%d_%dl_%dth.json", FILENAME, lineNumber, dcLength, threshold);
    }

    static String terminalFileName(int lineNumber, int dcLength, int threshold) {
        makePath();
        return String.format("./output/%s/terminal_%d_%dl_%dth.json", FILENAME, lineNumber, dcLength, threshold);
    }

    static String dcFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/dc_" + lineNumber + ".csv";
    }

    static String columnPairsFileName() {
        makePath();
        return "./output/" + FILENAME + "/columnPairs.json";
    }

    static String relationFileName() {
        return "./input/" + FILENAME + ".csv";
    }


    static void saveColumnPairs(List<DataPredicateGroup> predicateGroups) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(columnPairsFileName()))) {
            List<PredicateGroup.JsonDTO> data = predicateGroups.stream().filter(DataPredicateGroup::isMajor).map(DataPredicateGroup::toJsonDTO).collect(Collectors.toList());
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<AbstractPredicateGroup> loadAbstractPredicateGroups() throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(columnPairsFileName()));

            Type listType = new TypeToken<List<PredicateGroup.JsonDTO>>(){}.getType();
            List<PredicateGroup.JsonDTO> data = gson.fromJson(reader, listType);
            return AbstractPredicateGroup.fromJsonDTO(data);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<ColumnPair> loadColumnPairs(Relation relation) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(columnPairsFileName()));

            Type listType = new TypeToken<List<PredicateGroup.JsonDTO>>(){}.getType();
            List<PredicateGroup.JsonDTO> data = gson.fromJson(reader, listType);
            List<ColumnPair> columnPairs = new ArrayList<>();
            data.forEach(pair -> {
                try {
                    columnPairs.add(new ColumnPair(
                            relation.getAttribute(pair.getFirstColumn()),
                            relation.getAttribute(pair.getSecondColumn())
                    ));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return columnPairs;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void saveUncovered(int size, List<Evidence> evidence, int dcLength, int threshold) {
        saveEvidence(uncoveredEvidenceFileName(size, dcLength, threshold), evidence);
    }

    static List<Evidence> loadUncovered(int size, int dcLength, int threshold) throws IOException {
        List<Evidence> evidence = new ArrayList<>();
        if (size == 0) {
            return evidence;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(uncoveredEvidenceFileName(size, dcLength, threshold)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PredicateBitmap key = new PredicateBitmap();
                String[] parts = line.split(",");
                for (int i = 1; i < parts.length; i++) {
                    key.set(Integer.parseInt(parts[i].trim()));
                }
                evidence.add(new Evidence(key, Integer.parseInt(parts[0].trim())));
            }
        } catch (IOException e) {
            System.out.println("Evidence file " + evidenceFileName(size)  + " not found.");
            throw e;
        }
        return evidence;
    }

    static void saveEvidence(int size, Map<PredicateBitmap, Integer> evidence) {
        saveEvidence(evidenceFileName(size), Evidence.fromMap(evidence));
        System.out.println("不重复的证据行数: "+evidence.size());
    }

    static void saveEvidence(String fileName, List<Evidence> evidences) {
        evidences.sort(Evidence::compareTo);
        StringBuilder builder = new StringBuilder();
        for (Evidence evidence : evidences) {
            encode(builder, evidence.multiplicity(), evidence.predicates());
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            // no headline
            writer.write(builder.toString());
            System.out.println("Evidence Map saved to CSV file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static List<Evidence> loadEvidence(int size) throws IOException {
        List<Evidence> evidence = new ArrayList<>();
        if (size == 0) {
            return evidence;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(evidenceFileName(size)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PredicateBitmap key = new PredicateBitmap();
                String[] parts = line.split(",");
                for (int i = 1; i < parts.length; i++) {
                    key.set(Integer.parseInt(parts[i].trim()));
                }
                evidence.add(new Evidence(key, Integer.parseInt(parts[0].trim())));
            }
        } catch (IOException e) {
            System.out.println("Evidence file " + evidenceFileName(size)  + " not found.");
            throw e;
        }
        return evidence;
    }

    private static void saveCover(List<Cover> covers, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            gson.toJson(covers, writer);
            System.out.println("Cover saved to CSV file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void encode(StringBuilder builder, int value, PredicateBitmap key) {
        builder.append(value).append(", ");

        for (int i = key.nextSetBit(0); i >= 0; i = key.nextSetBit(i + 1)) {
            builder.append(i).append(", ");
        }

        // delete the last comma
        builder.delete(builder.length() - 2, builder.length()).append(System.lineSeparator());
    }

    static void saveCover(int size, List<Cover> cover, int dcLength, int threshold) {
        saveCover(cover, coverFileName(size, dcLength, threshold));
    }

    static void saveTerminal(int size, List<Cover> cover, int dcLength, int threshold) {
        saveCover(cover, terminalFileName(size, dcLength, threshold));
    }

    static List<Cover> loadCover(int size, int dcLength, int threshold) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(coverFileName(size, dcLength, threshold)));

            Type listType = new TypeToken<List<Cover>>(){}.getType();
            return gson.fromJson(reader, listType);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static List<Cover> loadTerminal(int size, int dcLength, int threshold) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(terminalFileName(size, dcLength, threshold)));

            Type listType = new TypeToken<List<Cover>>(){}.getType();
            return gson.fromJson(reader, listType);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void writeExpression(int size, List<Cover> covers) throws IOException {
        List<AbstractPredicate> allPredicates = loadAbstractPredicateGroups().get(0).allPredicates;
        StringBuilder builder = new StringBuilder();
        for (Cover dc : covers) {
            builder.append("NOT ");
            for (int i = dc.containing.nextSetBit(0); i >= 0; i = dc.containing.nextSetBit(i + 1)) {
                builder.append("(").append(allPredicates.get(i).getNegativeExpression()).append(") AND ");
            }
            builder.delete(builder.length() - 5, builder.length()).append(System.lineSeparator());
        }

        try (FileWriter writer = new FileWriter(dcFileName(size))) {
            writer.write(builder.toString());
        }
    }
}
