package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 10);
        Relation r = input.toRelation();
//        int size = predicates.size();
//        System.out.println(predicates.get(size-2));
//        System.out.println(predicates.get(size-1));
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSetNaive();
        EvidenceSetBuilder builder2 = new EvidenceSetBuilder(r);
        builder2.buildEvidenceSet();
        List<List<Predicate<?>>> es = builder2.getEvidenceSet();
        List<List<Predicate<?>>> es2 = builder.getEvidenceSet();
        for (int i = 0; i < es.size(); i++) {
            List<Predicate<?>> l1 = es.get(i);
            List<Predicate<?>> l2 = es2.get(i);
            System.out.println(l1.size() + " " + l2.size());
        }
    }
}
