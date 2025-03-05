package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.staticDC.DCFinder;
import incREE.staticDC.DCRanker;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Main {

    private static final int CURRENT_LINES = 15;
    private static final int INC_LINES = 5;

    public static void main(String[] args) {

        Input input = new Input("adult.csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e1 = builder.collect();
//        DCFinder dcFinder = new DCFinder(0.1, r.getTotalTuplePairs(), r.predicateSpace, e1);
//        List<List<Predicate<?>>> cover = dcFinder.findCover();

        IncEvidenceSetBuilder incEvidence = new IncEvidenceSetBuilder(r, e1, INC_LINES);
        e1 = incEvidence.build();

        input = new Input("adult.csv");
        Relation r2 = input.getRelation(CURRENT_LINES+INC_LINES);
        EvidenceSetBuilder builder2 = new EvidenceSetBuilder(r2);
        builder2.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e2 = builder2.collect();

        if (Checker.checkEvidenceSet(e1, e2)) {
            System.out.println("Success.");
        }

//        cover.forEach(dc -> Checker.checkDCNaive(r, dc));
//        DCRanker ranker = new DCRanker(cover, er);
//        ranker.getRankedDC();
    }
}
