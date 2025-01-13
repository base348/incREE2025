package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.Checker;
import incREE.evidence.Evidence;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;
import incREE.staticDC.DCFinder;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 200);
        Relation r = input.toRelation();
        DCFinder dcFinder = new DCFinder(0.1, r);
        List<List<Predicate<?>>> cover = dcFinder.findCover();
        for (List<Predicate<?>> dc : cover) {
            Checker.checkDCNaive(r, dc);
        }
    }
}
