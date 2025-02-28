package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.Checker;
import incREE.evidence.Evidence;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.staticDC.DCFinder;
import incREE.staticDC.DCRanker;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Main {

    private static final int CURRENT_LINES = 5;
    private static final int INC_LINES = 10;

    public static void main(String[] args) {

        Input input = new Input("adult.csv", CURRENT_LINES);
        Relation r = input.getRelation();
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        List<Evidence> e1 = builder.collect();
        DCFinder dcFinder = new DCFinder(0.1, r.getTotalTuplePairs(), r.predicateSpace, e1);
        List<List<Predicate<?>>> cover = dcFinder.findCover();
        IncEvidenceSetBuilder incEvidence = new IncEvidenceSetBuilder(r, INC_LINES);
//        cover.forEach(dc -> Checker.checkDCNaive(r, dc));
//        DCRanker ranker = new DCRanker(cover, er);
//        ranker.getRankedDC();
    }
}
