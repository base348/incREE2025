package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;

public class Main {
    public static void main(String[] args) {
        Input input = new Input("adult.csv", 30);
        Relation r = input.toRelation();
        r.print();
    }
}