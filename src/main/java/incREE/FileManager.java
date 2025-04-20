package incREE;

import com.google.gson.reflect.TypeToken;
import incREE.dataset.ColumnPair;
import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileManager {
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static final String FILENAME = "atom";

    private static void makePath() {
        File targetDir = new File("./output/" + FILENAME);
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs();
        }
    }

    static String evidenceFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/evidence_" + lineNumber + ".csv";
    }

    static String coverFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/cover_" + lineNumber + ".csv";
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

    public static List<AbstractPredicateGroup> loadAbstractPredicateGroups() {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(columnPairsFileName()));

            Type listType = new TypeToken<List<PredicateGroup.JsonDTO>>(){}.getType();
            List<PredicateGroup.JsonDTO> data = gson.fromJson(reader, listType);
            return AbstractPredicateGroup.fromJsonDTO(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    static void saveEvidence(int size, Map<PredicateBitmap, Integer> evidence) {
//        StringBuilder builder = new StringBuilder();
//
//        for (Map.Entry<PredicateBitmap, Integer> entry : evidence.entrySet()) {
//            PredicateBitmap key = entry.getKey();
//            int value = entry.getValue();
//
//            builder.append(value).append(", ");
//
//            for (int i = key.nextSetBit(0); i >= 0; i = key.nextSetBit(i + 1)) {
//                builder.append(i).append(", ");
//            }
//
//            // delete the last comma
//            builder.delete(builder.length() - 2, builder.length()).append(System.lineSeparator());
//        }
//
//        try (FileWriter writer = new FileWriter(evidenceFileName(size))) {
//            // no headline
//            writer.write(builder.toString());
//            System.out.println("Evidence Map saved to CSV file: " + evidenceFileName(size));
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }
        saveEvidence(size, Evidence.fromMap(evidence));
    }

    static void saveEvidence(int size, List<Evidence> evidences) {
        evidences.sort(Evidence::compareTo);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < evidences.size(); i++) {
            PredicateBitmap key = evidences.get(i).predicates();
            int value = evidences.get(i).multiplicity();
            builder.append(value).append(", ");

            for (int j = key.nextSetBit(0); j >= 0; j = key.nextSetBit(j + 1)) {
                builder.append(j).append(", ");
            }

            // delete the last comma
            builder.delete(builder.length() - 2, builder.length()).append(System.lineSeparator());
        }

        try (FileWriter writer = new FileWriter(evidenceFileName(size))) {
            // no headline
            writer.write(builder.toString());
            System.out.println("Evidence Map saved to CSV file: " + evidenceFileName(size));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static List<Evidence> loadEvidence(int size) throws IOException {
        List<Evidence> evidence = new ArrayList<>();
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

    static void saveCover(int size, List<PredicateBitmap> cover) {
        String filename = coverFileName(size);
        try (FileWriter writer = new FileWriter(filename)) {
            // no headline
            for (PredicateBitmap entry : cover) {
                writer.write(entry + "\n");
            }

            System.out.println("Evidence Map saved to CSV file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static void writeExpression(String filename) {
        List<PredicateBitmap> cover = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename + "_Cover.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PredicateBitmap bitset = new PredicateBitmap();
                String[] parts = line.split(",");
                for (String part : parts) {
                    bitset.set(Integer.parseInt(part.trim()));
                }
                cover.add(bitset);
            }
        } catch (IOException e) {
            System.out.println("File " + filename  + " not found.");
            throw new RuntimeException(e);
        }

        Input input = new Input(filename + ".csv");
        Relation r = input.getRelation(1);
        StringBuilder builder = new StringBuilder();
        for (PredicateBitmap dc : cover) {
            builder.append("NOT ");
            for (int i = dc.nextSetBit(0); i >= 0; i = dc.nextSetBit(i + 1)) {
                builder.append("(").append(r.predicateSpace.get(i).getNegativeExpression()).append(") AND ");
            }
            builder.delete(builder.length() - 5, builder.length()).append(System.lineSeparator());
        }

        try (FileWriter writer = new FileWriter(filename + "_DCExpressions.csv")) {
            writer.write(builder.toString());
        } catch (IOException e) {
            System.out.println("File " + filename  + " not found.");
            throw new RuntimeException(e);
        }
    }
}
