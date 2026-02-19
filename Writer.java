import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Writer {
    private Config cfg;
    private Stats stats;

    public Writer(Config cfg) {
        this.cfg = cfg;
        this.stats = new Stats(cfg);
    }

    public void write(List<Article> arts, int total, ExecutorService exec, int threads)
            throws InterruptedException, ExecutionException {

        CyclicBarrier bar = new CyclicBarrier(threads);

        ConcurrentHashMap<String, List<String>> cats = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<String>> langs = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> words = new ConcurrentHashMap<>();

        int n = arts.size();
        int p = threads;

        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            final int start = (int)(tid * (double)n / p);
            final int end = (int)Math.min((tid + 1) * (double)n / p, n);

            Future<?> future = exec.submit(() -> {
                try {
                    for (int i = start; i < end; i++) {
                        Article a = arts.get(i);
                        processCategories(a, cats);
                        processLanguages(a, langs);
                        processKeywords(a, words);
                    }

                    bar.await();
                    if (tid == 0) {
                        sort(arts);
                        writeMain(arts);
                        writeStats(arts, total, words);
                    }

                    bar.await();
                    writeCats(tid, p, cats);
                    writeLangs(tid, p, langs);

                    if (tid == 0) {
                        writeWords(words);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            futures.add(future);
        }

        for (Future<?> f : futures) {
            f.get();
        }
    }

    private void processCategories(Article a, ConcurrentHashMap<String, List<String>> cats) {
        if (a.getCategories() != null) {
            Set<String> verif = new HashSet<>();
            for (int j = 0; j < a.getCategories().size(); j++) {
                String cat = a.getCategories().get(j);
                String clean = cat.trim();
                if (!verif.contains(clean)) {
                    verif.add(clean);
                    String norm = stats.norm(clean);
                    synchronized (cats) {
                        if (!cats.containsKey(norm)) {
                            cats.put(norm, new CopyOnWriteArrayList<>());
                        }
                    }
                    List<String> list = cats.get(norm);
                    list.add(a.getUuid());
                }
            }
        }
    }

    private void processLanguages(Article a, ConcurrentHashMap<String, List<String>> langs) {
        String lang = a.getLanguage();
        if (lang != null && cfg.getLangs().contains(lang)) {
            langs.computeIfAbsent(lang, k -> new CopyOnWriteArrayList<>()).add(a.getUuid());
        }
    }

    private void processKeywords(Article a, ConcurrentHashMap<String, Set<String>> words) {
        if ("english".equals(a.getLanguage()) && a.getText() != null) {
            String txt = a.getText().toLowerCase();
            String[] toks = txt.split("\\s+");

            for (int j = 0; j < toks.length; j++) {
                String tok = toks[j];
                String w = tok.replaceAll("[^a-z]", "");
                if (w.isEmpty() || cfg.getWords().contains(w)) continue;

                words.computeIfAbsent(w, k -> ConcurrentHashMap.newKeySet()).add(a.getUuid());
            }
        }
    }

    private void sort(List<Article> arts) {
        Collections.sort(arts, (a, b) -> {
            String p1;
            if (a.getPublished() == null) {
                p1 = "";
            } else {
                p1 = a.getPublished();
            }

            String p2;
            if (b.getPublished() == null) {
                p2 = "";
            } else {
                p2 = b.getPublished();
            }
            int cmp = p2.compareTo(p1);
            if (cmp != 0) return cmp;
            return a.getUuid().compareTo(b.getUuid());
        });
    }

    private void writeMain(List<Article> arts) throws IOException {
        try (PrintWriter pw = new PrintWriter("all_articles.txt")) {
            for (int i = 0; i < arts.size(); i++) {
                Article a = arts.get(i);
                pw.println(a.getUuid() + " " + a.getPublished());
            }
        }
    }

    private void writeCats(int tid, int p, ConcurrentHashMap<String, List<String>> map)
            throws IOException {
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        int n = keys.size();
        int start = (int)(tid * (double)n / p);
        int end = (int)Math.min((tid + 1) * (double)n / p, n);

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            List<String> vals = new ArrayList<>(map.get(key));
            Collections.sort(vals);
            try (PrintWriter pw = new PrintWriter(key + ".txt")) {
                for (int j = 0; j < vals.size(); j++) {
                    pw.println(vals.get(j));
                }
            }
        }
    }

    private void writeLangs(int tid, int p, ConcurrentHashMap<String, List<String>> map)
            throws IOException {
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        int n = keys.size();
        int start = (int)(tid * (double)n / p);
        int end = (int)Math.min((tid + 1) * (double)n / p, n);

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            List<String> vals = new ArrayList<>(map.get(key));
            Collections.sort(vals);
            try (PrintWriter pw = new PrintWriter(key + ".txt")) {
                for (int j = 0; j < vals.size(); j++) {
                    pw.println(vals.get(j));
                }
            }
        }
    }

    private void writeWords(ConcurrentHashMap<String, Set<String>> map) throws IOException {
        List<Map.Entry<String, Set<String>>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> {
            int c = Integer.compare(b.getValue().size(), a.getValue().size());
            if (c != 0) {
                return c;
            } else {
                return a.getKey().compareTo(b.getKey());
            }

        });

        try (PrintWriter pw = new PrintWriter("keywords_count.txt")) {
            for (int i = 0; i < list.size(); i++) {
                Map.Entry<String, Set<String>> e = list.get(i);
                if (!e.getValue().isEmpty()) {
                    pw.println(e.getKey() + " " + e.getValue().size());
                }
            }
        }
    }

    private void writeStats(List<Article> arts, int total, Map<String, Set<String>> words)
            throws IOException {
        int dups = stats.duplicates_found(total, arts.size());
        int unique = stats.unique_articles(arts);

        String auth = stats.bestAuthor(arts);
        int authCnt = stats.countAuthor(arts, auth);

        String lang = stats.top_language(arts);
        int langCnt = stats.count_top_language(arts, lang);

        String cat = stats.top_category(arts);
        int catCnt = stats.count_top_category(arts, cat);

        Article rec = stats.most_recent_article(arts);

        String word = stats.top_keyword_en(words);
        int wordCnt = words.getOrDefault(word, new HashSet<>()).size();

        try (PrintWriter pw = new PrintWriter("reports.txt")) {
            pw.println("duplicates_found - " + dups);
            pw.println("unique_articles - " + unique);
            if (!auth.isEmpty()) pw.println("best_author - " + auth + " " + authCnt);
            if (!lang.isEmpty()) pw.println("top_language - " + lang + " " + langCnt);
            if (!cat.isEmpty()) pw.println("top_category - " + cat + " " + catCnt);
            if (rec != null) pw.println("most_recent_article - " + rec.getPublished() + " " + rec.getUrl());
            if (!word.isEmpty()) pw.println("top_keyword_en - " + word + " " + wordCnt);
        }
    }
}
