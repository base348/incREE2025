package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.Predicate;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static Relation relation;
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 100);
        Relation r = input.toRelation();
        relation = r;
        List<Predicate> predicates = Predicate.getPredicatesSpace(r);
//        int size = predicates.size();
//        System.out.println(predicates.get(size-2));
//        System.out.println(predicates.get(size-1));
        System.out.println(predicates);
    }
}
