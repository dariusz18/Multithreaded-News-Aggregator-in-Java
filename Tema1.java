import java.util.*;
import java.util.concurrent.*;

public class Tema1 {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("invalid");
            System.exit(1);
        }

        int threads = Integer.parseInt(args[0]);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            Config cfg = new Config(threads);
            cfg.loadFiles(args[1]);
            cfg.loadInputs(args[2]);

            Reader reader = new Reader();
            reader.parseAll(cfg.getFiles(), executor, threads);

            List<Article> all = new ArrayList<>(reader.getData());
            List<Article> clean = new Cleaner().clean(all, executor, threads);

            new Writer(cfg).write(clean, all.size(), executor, threads);

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
