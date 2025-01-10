package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 100);
        Relation r = input.toRelation();
//        int size = predicates.size();
//        System.out.println(predicates.get(size-2));
//        System.out.println(predicates.get(size-1));
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.test();
    }
}
