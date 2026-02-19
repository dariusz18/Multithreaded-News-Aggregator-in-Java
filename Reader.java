import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class Reader {
    private static final ObjectMapper map = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ConcurrentLinkedQueue<Article> data;

    public Reader() {
        this.data = new ConcurrentLinkedQueue<>();
    }

    public void parseAll(List<String> files, ExecutorService exec, int threads)
            throws InterruptedException, ExecutionException {

        int n = files.size();
        int p = threads;

        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < p; t++) {
            final int tid = t;
            final int start = (int)(tid * (double)n / p);
            final int end = (int)Math.min((tid + 1) * (double)n / p, n);

            Future<?> future = exec.submit(() -> {
                for (int i = start; i < end; i++) {
                    String file = files.get(i);
                    try {
                        parse(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            futures.add(future);
        }

        for (Future<?> f : futures) {
            f.get();
        }
    }

    private void parse(String path) throws IOException {
        File f = new File(path);
        List<Article> articles = map.readValue(f, new TypeReference<List<Article>>() {});
        if (articles != null) {
            data.addAll(articles);
        }
    }

    public ConcurrentLinkedQueue<Article> getData() {
        return data;
    }
}
