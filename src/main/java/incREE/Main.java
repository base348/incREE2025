package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.Checker;
import incREE.evidence.Evidence;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;
import incREE.staticDC.DCFinder;
import incREE.staticDC.DCRanker;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 2000);
        Relation r = input.toRelation();
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        List<Evidence> er = builder.getDegenerateEvidenceSet();
        DCFinder dcFinder = new DCFinder(0.1, r, er);
        List<List<Predicate<?>>> cover = dcFinder.findCover();
        DCRanker ranker = new DCRanker(cover, er);
        ranker.getRankedDC();
    }
}
