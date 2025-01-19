package incREE.staticDC;

import incREE.evidence.Evidence;
import incREE.evidence.Predicate;

import java.util.ArrayList;
import java.util.List;

public class DCRanker {

    private static record Score(int score, int index) {
    }

    private final List<List<Predicate<?>>> dc;
    private final List<Score> coverage;
    private final List<Evidence> er;

    public DCRanker(List<List<Predicate<?>>> dc, List<Evidence> er) {
        this.dc = dc;
        this.coverage = new ArrayList<>();
        this.er = er;
    }

    private int getScore(List<Predicate<?>> dc) {
        int amount = 0;
        int amountSub;
        for (Predicate<?> predicate : dc) {
            List<Predicate<?>> sub = new ArrayList<>(dc);
            sub.remove(predicate);
            amountSub = 0;
            for (Evidence evidence : er) {
                if (evidence.satisfied(sub)) {
                    amountSub += evidence.multiplicity();
                }
            }
            amount = Math.max(amount, amountSub);
        }
        return amount;
    }

    private void calculateScore() {
        for (int i = 0; i < dc.size(); i++) {
            coverage.add(new Score(getScore(dc.get(i)), i));
        }
        coverage.sort( (o1, o2) -> o1.score - o2.score );
    }

    public List<List<Predicate<?>>> getRankedDC() {
        List<List<Predicate<?>>> ranked = new ArrayList<>();
        calculateScore();
        for (Score score : coverage) {
            ranked.add(dc.get(score.index));
            System.out.println(dc.get(score.index) + " Score=" + score.score);
        }
        return ranked;
    }
}
