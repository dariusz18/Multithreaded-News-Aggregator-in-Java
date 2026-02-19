import java.util.*;
import java.util.concurrent.*;

public class Cleaner {

    public List<Article> clean(List<Article> all, ExecutorService exec, int threads)
            throws InterruptedException, ExecutionException {

        int n = all.size();
        int p = threads;

        ConcurrentHashMap<String, Integer> uuids = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> titles = new ConcurrentHashMap<>();
        CyclicBarrier bar = new CyclicBarrier(threads);
        List<List<Article>> parts = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            parts.add(new ArrayList<>());
        }

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            final int start = (int)(tid * (double)n / p);
            final int end = (int)Math.min((tid + 1) * (double)n / p, n);
            final List<Article> part = parts.get(tid);

            Future<?> future = exec.submit(() -> {
                try {
                    for (int i = start; i < end; i++) {
                        Article a = all.get(i);
                        if (a.getUuid() != null) {
                            uuids.merge(a.getUuid(), 1, Integer::sum);
                        }
                        if (a.getTitle() != null) {
                            titles.merge(a.getTitle(), 1, Integer::sum);
                        }
                    }
                    bar.await();

                    for (int i = start; i < end; i++) {
                        Article a = all.get(i);
                        if (a.getUuid() == null || a.getTitle() == null) continue;
                        String uuid = a.getUuid();
                        String title = a.getTitle();
                        if (uuids.get(uuid) == 1 &&  titles.get(title) == 1) {
                            part.add(a);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }

        List<Article> result = new ArrayList<>();
        for (List<Article> part : parts) {
            result.addAll(part);
        }

        return result;
    }
}
