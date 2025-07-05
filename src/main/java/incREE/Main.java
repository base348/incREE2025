package incREE;

public class Main {

    public static void main(String[] args) {
        String db = "adult";
        String mode = "static";
        int current = 1000;
        int inc = 50;
        int errThreshold = 5;
        int dcLength = 6;

        if (args.length > 0) db = args[0];
        if (args.length > 1) mode = args[1];
        try {
            if (args.length > 2) current = Integer.parseInt(args[2]);
            if (mode.equals("inc") && args.length > 3) inc = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("Error parsing arguments, using default values");
        }

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--errThreshold=")) {
                    errThreshold = Integer.parseInt(arg.substring(15));
                } else if (arg.startsWith("--dcLength=")) {
                    dcLength = Integer.parseInt(arg.substring(11));
                }
            }
        }

        FileManager.setFilename(db);

        if (mode.equals("inc")) {
            DCFinder.incDC(current, inc, errThreshold, dcLength);
        } else {
            DCFinder.staticDC(current, errThreshold, dcLength);
        }
    }
}
