package incREE;

import com.google.gson.reflect.TypeToken;
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
import incREE.staticDC.CoverFinder;

public class FileManager {
    static Gson gson = new GsonBuilder()
            .registerTypeAdapter(PredicateBitmap.class, new PredicateBitmapAdapter())
            .create();
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

    static String terminalFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/terminal_" + lineNumber + ".csv";
    }

    static String dcFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/dc_" + lineNumber + ".csv";
    }

    static String columnPairsFileName() {
        makePath();
        return "./output/" + FILENAME + "/columnPairs.json";
    }

    static String coverJsonFileName(int lineNumber) {
        makePath();
        return "./output/" + FILENAME + "/cover_" + lineNumber + ".json";
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

    static void saveEvidence(int size, Map<PredicateBitmap, Integer> evidence) {
        saveEvidence(size, Evidence.fromMap(evidence));
    }

    static void saveEvidence(int size, List<Evidence> evidences) {
        evidences.sort(Evidence::compareTo);
        StringBuilder builder = new StringBuilder();
        for (Evidence evidence : evidences) {
            encode(builder, evidence.multiplicity(), evidence.predicates());
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

    private static void saveCover(List<CoverFinder.Cover> covers, String filename) {
        StringBuilder builder = new StringBuilder();
        for (CoverFinder.Cover cover : covers) {
            encode(builder, cover.uncovered, cover.containing);
        }
        try (FileWriter writer = new FileWriter(filename)) {
            // no headline
            writer.write(builder.toString());
            System.out.println("Evidence Map saved to CSV file: " + filename);
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

    static void saveCover(int size, CoverFinder.Result cover) {
        saveCover(cover.covers, coverFileName(size));
    }

    static void saveTerminal(int size, CoverFinder.Result cover) {
        saveCover(cover.terminals, terminalFileName(size));
    }

    static void trailSave(int size, CoverFinder.Result cover) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(coverJsonFileName(size)))) {
            gson.toJson(cover.covers, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void trailLoad(int size) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(coverJsonFileName(size)));

            Type listType = new TypeToken<List<CoverFinder.Cover>>(){}.getType();
            List<PredicateGroup.JsonDTO> data = gson.fromJson(reader, listType);
//            return AbstractPredicateGroup.fromJsonDTO(data);
            System.out.println("Successfully loaded column pairs.");
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

//    static CoverFinder.Result loadCover(int size) throws IOException {
//        CoverFinder.Result result = new CoverFinder.Result();
//        BufferedReader reader;
//        String line;
//        try {
//            reader  = new BufferedReader(new FileReader(coverFileName(size)));
//            while ((line = reader.readLine()) != null) {
//                PredicateBitmap cover = new PredicateBitmap();
//                String[] parts = line.split(",");
//                for (int i = 1; i < parts.length; i++) {
//                    cover.set(Integer.parseInt(parts[i].trim()));
//                }
//                result.covers.add(new CoverFinder.Cover(cover, Integer.parseInt(parts[0].trim())));
//            }
//
//            reader  = new BufferedReader(new FileReader(terminalFileName(size)));
//            while ((line = reader.readLine()) != null) {
//                PredicateBitmap cover = new PredicateBitmap();
//                String[] parts = line.split(",");
//                for (int i = 1; i < parts.length; i++) {
//                    cover.set(Integer.parseInt(parts[i].trim()));
//                }
//                result.terminals.add(new CoverFinder.Cover(cover, Integer.parseInt(parts[0].trim())));
//            }
//        } catch (IOException e) {
//            System.out.println("Evidence file " + evidenceFileName(size)  + " not found.");
//            throw e;
//        }
//        return result;
//    }

    static void writeExpression(int size, CoverFinder.Result cover) throws IOException {
        List<AbstractPredicate> allPredicates = loadAbstractPredicateGroups().get(0).allPredicates;
        StringBuilder builder = new StringBuilder();
        for (CoverFinder.Cover dc : cover.covers) {
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
