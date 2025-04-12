package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.staticDC.CoverFinder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    private static final int CURRENT_LINES = 20000;
    private static final int INC_LINES = 20000;
    private static final String FILENAME = "atom";

    private static void saveEvidence(String filename, Map<PredicateBitmap, Integer> evidence) {

        try (FileWriter writer = new FileWriter(filename)) {
            // no headline
            for (Map.Entry<PredicateBitmap, Integer> entry : evidence.entrySet()) {
                PredicateBitmap key = entry.getKey();
                int value = entry.getValue();

                writer.write(value + ", " + key + "\n");
            }

            System.out.println("Evidence Map saved to CSV file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void saveDC(String filename, List<PredicateBitmap> cover) {
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

    private static Map<PredicateBitmap, Integer> loadEvidence(String filename) throws IOException {
        Map<PredicateBitmap, Integer> evidence = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PredicateBitmap key = new PredicateBitmap();
                String[] parts = line.split(",");
                for (int i = 1; i < parts.length; i++) {
                    key.set(Integer.parseInt(parts[i].trim()));
                }
                evidence.put(key, Integer.parseInt(parts[0].trim()));
            }
        } catch (IOException e) {
            System.out.println("File " + filename  + " not found.");
            throw e;
        }
        return evidence;
    }

    private static void writeExpression(String filename) {
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
        Relation r = input.getRelation(CURRENT_LINES);
        StringBuilder builder = new StringBuilder();
        for (PredicateBitmap dc : cover) {
            builder.append("NOT ");
            for (int i = dc.nextSetBit(0); i >= 0; i = dc.nextSetBit(i + 1)) {
                builder.append("(").append(r.predicateSpace.get(i).getNegativeExpression()).append(") AND ");
            }
            builder.delete(builder.length() - 5, builder.length()).append("\n");
        }

        try (FileWriter writer = new FileWriter(filename + "_DCExpressions.csv")) {
            writer.write(builder.toString());
        } catch (IOException e) {
            System.out.println("File " + filename  + " not found.");
            throw new RuntimeException(e);
        }
    }

    private static void merge(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> map1.merge(key, value, Integer::sum));
    }

    private static void mergeAndSave(String filename) {
        Map<PredicateBitmap, Integer> evidence;
        Map<PredicateBitmap, Integer> incEvidence;
        try {
            evidence = loadEvidence(evidenceFileName(filename, CURRENT_LINES));
        } catch (IOException e) {
            System.out.println("Building the required evidence map instead.");
            evidence = buildEvidenceSet(filename);
        }
        try {
            incEvidence = loadEvidence(incEvidenceFileName(filename, CURRENT_LINES, INC_LINES));
        } catch (IOException e) {
            System.out.println("Building the required evidence map instead.");
            incEvidence = buildIncEvidenceSet(filename);
        }
        merge(evidence, incEvidence);
        saveEvidence(evidenceFileName(filename, CURRENT_LINES + INC_LINES), evidence);
    }

    private static String evidenceFileName(String filename, int lineNumber) {
        return "evidence_" + filename + "_" + lineNumber + ".csv";
    }
    private static String incEvidenceFileName(String filename, int lineNumber, int incLineNumber) {
        return "evidence_" + filename + "_" + lineNumber + "_to_" + (lineNumber + incLineNumber) + ".csv";
    }



    private static void test() {
        Input input = new Input("adult.csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e = builder.collect();
        System.out.println("Evidence Set build complete.");
//        DCFinder dcFinder = new DCFinder(0.1, r.getTotalTuplePairs(), r.predicateSpace, e1);
//        List<List<Predicate<?>>> cover = dcFinder.findCover();

        IncEvidenceSetBuilder incEvidence = new IncEvidenceSetBuilder(r, INC_LINES);
        Map<PredicateBitmap, Integer> eInc = incEvidence.build();
        System.out.println("Inc Evidence Set build complete.");

//        CoverFinder dcFinder = new CoverFinder(0, r.getTotalTuplePairs(), e);
//        List<List<Predicate<?>>> cover = dcFinder.findCover();
//        System.out.println("DC Finder complete.");

//        DynEI incDC = new DynEI(e, eInc, r.predicateSpace, cover);
//        incDC.DynDC();

//        cover.forEach(dc -> Checker.checkDCNaive(r, dc));
//        DCRanker ranker = new DCRanker(cover, er);
//        ranker.getRankedDC();
    }

    private static void buildEvidenceSetAndSave(String filename) {
        Map<PredicateBitmap, Integer> e = buildEvidenceSet(filename);
        saveEvidence(evidenceFileName(filename, CURRENT_LINES), e);
    }

    private static Map<PredicateBitmap, Integer> buildEvidenceSet(String filename) {
        Input input = new Input(filename + ".csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        return builder.collect();
    }

    private static void buildIncEvidenceSetAndSave(String filename) {
        Map<PredicateBitmap, Integer> incEvidence = buildIncEvidenceSet(filename);
        saveEvidence(incEvidenceFileName(filename, CURRENT_LINES, INC_LINES), incEvidence);
    }

    private static Map<PredicateBitmap, Integer> buildIncEvidenceSet(String filename) {
        Input input = new Input(filename + ".csv");
        Relation r = input.getRelation(CURRENT_LINES, INC_LINES);
        IncEvidenceSetBuilder builder = new IncEvidenceSetBuilder(r, INC_LINES);
        return builder.build();
    }

    private static void saveColumnPairs() {
        Input input = new Input(FILENAME + ".csv");
        Relation r = input.getRelation(CURRENT_LINES + INC_LINES);
        r.buildPredicateSpace();
        List<PredicateGroup> predicateGroups = r.predicateGroups;

        StringBuilder builder = new StringBuilder();
        for (PredicateGroup predicateGroup : predicateGroups) {
            builder.append(predicateGroup.toString());
            builder.append(System.lineSeparator());
        }
        System.out.println("Saving predicate groups.");
        try (FileWriter writer = new FileWriter(FILENAME + "_ColumnGroups.csv")) {
            writer.write(builder.toString());
        } catch (IOException e) {
            System.out.println("File " + FILENAME  + " not found.");
            throw new RuntimeException(e);
        }
    }

    private static void findCover(String filename) {
        try {
            Map<PredicateBitmap, Integer> evidence = loadEvidence(evidenceFileName(filename, CURRENT_LINES));
            Input input = new Input(filename + ".csv");
            Relation r = input.getRelation(0);
            CoverFinder coverFinder = new CoverFinder(0, r.getTotalTuplePairs(), evidence, r.predicateGroups);
            List<PredicateBitmap> cover = coverFinder.findCover();
            System.out.println("Cover find complete.");
            saveDC(filename + "_Cover.csv", cover);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        saveColumnPairs();
//        mergeAndSave(FILENAME);
//        findCover(FILENAME);
//        writeExpression(FILENAME);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");
    }
}
