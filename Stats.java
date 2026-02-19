import java.util.*;

public class Stats {
    private Config cfg;

    public Stats(Config cfg) {
        this.cfg = cfg;
    }

    public int duplicates_found(int total, int unique) {
        return total - unique;
    }

    public int unique_articles(List<Article> arts) {
        return arts.size();
    }

    public String bestAuthor(List<Article> arts) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < arts.size(); i++) {
            Article a = arts.get(i);
            if (a.getAuthor() != null) {
                map.put(a.getAuthor(), map.getOrDefault(a.getAuthor(), 0) + 1);
            }
        }
        String autor = null;
        int maxi = -1;

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > maxi) {
                maxi = entry.getValue();
                autor = entry.getKey();
            } else if (maxi == entry.getValue()) {
                if (autor.compareTo(entry.getKey()) > 0) {
                    autor = entry.getKey();
                }
            }
        }
        return autor;
    }

    public int countAuthor(List<Article> arts, String auth) {
        int cnt = 0;
        for (int i = 0; i < arts.size(); i++) {
            if (auth.equals(arts.get(i).getAuthor())) {
                cnt++;
            }
        }
        return cnt;
    }

    public String top_language(List<Article> arts) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < arts.size(); i++) {
            Article a = arts.get(i);
            if (a.getLanguage() != null) {
                map.put(a.getLanguage(), map.getOrDefault(a.getLanguage(), 0) + 1);
            }
        }
        String  language = null;
        int maxi = -1;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > maxi) {
                maxi = entry.getValue();
                language = entry.getKey();
            } else if (maxi == entry.getValue()) {
                if (language.compareTo(entry.getKey()) > 0) {
                    language = entry.getKey();
                }
            }
        }
        return language;
    }

    public int count_top_language(List<Article> arts, String lang) {
        int cnt = 0;
        for (int i = 0; i < arts.size(); i++) {
            if (lang.equals(arts.get(i).getLanguage())) cnt++;
        }
        return cnt;
    }

    public String top_category(List<Article> arts) {
        Map<String, Set<String>> map = new HashMap<>();
        for (int i = 0; i < arts.size(); i++) {
            Article a = arts.get(i);
            if (a.getCategories() != null) {
                Set<String> verif = new HashSet<>();
                for (int j = 0; j < a.getCategories().size(); j++) {
                    String c = a.getCategories().get(j);
                    String clean = c.trim();
                    if (!verif.contains(clean)) {
                        verif.add(clean);
                        String norm = norm(clean);
                        map.putIfAbsent(norm, new HashSet<>());
                        map.get(norm).add(a.getUuid());
                    }
                }
            }
        }
        String category = null;
        int maxi = -1;

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            int count = entry.getValue().size();
            if (count > maxi) {
                maxi = count;
                category = entry.getKey();
            } else if (count == maxi) {
                if (category.compareTo(entry.getKey()) > 0) {
                    category = entry.getKey();
                }
            }
        }
        return category;
    }

    public int count_top_category(List<Article> arts, String cat) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < arts.size(); i++) {
            Article a = arts.get(i);
            if (a.getCategories() != null) {
                Set<String> verif = new HashSet<>();
                for (int j = 0; j < a.getCategories().size(); j++) {
                    String c = a.getCategories().get(j);
                    String clean = c.trim();
                    if (!verif.contains(clean)) {
                        verif.add(clean);
                        if (cat.equals(norm(clean))) {
                            set.add(a.getUuid());
                        }
                    }
                }
            }
        }
        return set.size();
    }

    public Article most_recent_article(List<Article> arts) {
        Article best = null;
        String bestDate = null;

        for (int i = 0; i < arts.size(); i++) {
            Article a = arts.get(i);
            String date = a.getPublished() == null ? "" : a.getPublished();

            if (best == null) {
                best = a;
                bestDate = date;
            } else {
                int c = date.compareTo(bestDate);
                if (c > 0) {
                    best = a;
                    bestDate = date;
                } else if (c == 0) {
                    if (a.getUuid().compareTo(best.getUuid()) > 0) {
                        best = a;
                        bestDate = date;
                    }
                }
            }
        }
        return best;
    }


    public String top_keyword_en(Map<String, Set<String>> map) {
        String best = null;
        int maxi = -1;

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            int count = entry.getValue().size();

            if (count > maxi) {
                maxi = count;
                best = entry.getKey();
            } else if (count == maxi) {
                if (best.compareTo(entry.getKey()) > 0) {
                    best = entry.getKey();
                }
            }
        }
        if (best == null) {
            return "";
        } else {
            return best;
        }
    }

    public String norm(String cat) {
        return cat.replace(",", "").replaceAll("\\s+", "_");
    }
}
