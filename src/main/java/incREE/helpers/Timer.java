package incREE.helpers;

public class Timer {
    private long startTime;
    private long evidenceSetTime;
    private long endTime;

    public Timer() {
        start();
    }

    private void start() {
        startTime = System.currentTimeMillis();
    }

    public void evidenceSetComplete() {
        evidenceSetTime = System.currentTimeMillis();
        System.out.println("Evidence Set building complete.");
    }

    public void end() {
        endTime = System.currentTimeMillis();
        print();
    }

    private void print() {
        System.out.println("\nBuild Evidence Set: " + (evidenceSetTime - startTime) + "ms;");
        System.out.println("Find Cover: " + (endTime - evidenceSetTime) + "ms;");
        System.out.println("Total: " + (endTime - startTime) + "ms.\n");
    }
}
