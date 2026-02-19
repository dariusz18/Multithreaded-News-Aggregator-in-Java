import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Config {
    private int threads;
    private List<String> files = new ArrayList<>();
    private Set<String> langs = ConcurrentHashMap.newKeySet();
    private Set<String> cats = ConcurrentHashMap.newKeySet();
    private Set<String> words = ConcurrentHashMap.newKeySet();

    public Config(int threads) {
        this.threads = threads;
    }

    public void loadFiles(String path) throws IOException {
        File directory = new File(path).getParentFile();
        try (BufferedReader buffer = new BufferedReader(new FileReader(path))) {
            int n = Integer.parseInt(buffer.readLine().trim());
            for (int i = 0; i < n; i++) {
                files.add(new File(directory, buffer.readLine().trim()).getPath());
            }
        }
    }

    public void loadInputs(String path) throws IOException {
        File directory = new File(path).getParentFile();
        try (BufferedReader buffer = new BufferedReader(new FileReader(path))) {
            buffer.readLine();
            loadSet(new File(directory, buffer.readLine().trim()).getPath(), langs);
            loadSet(new File(directory, buffer.readLine().trim()).getPath(), cats);
            loadSetLower(new File(directory, buffer.readLine().trim()).getPath(), words);
        }
    }

    private void loadSet(String path, Set<String> set) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new FileReader(path))) {
            int n = Integer.parseInt(buffer.readLine().trim());
            for (int i = 0; i < n; i++) {
                String line = buffer.readLine();
                if (line != null) set.add(line.trim());
            }
        }
    }

    private void loadSetLower(String path, Set<String> set) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new FileReader(path))) {
            int n = Integer.parseInt(buffer.readLine().trim());
            for (int i = 0; i < n; i++) {
                String line = buffer.readLine();
                if (line != null) set.add(line.trim().toLowerCase());
            }
        }
    }

    public List<String> getFiles() { return files; }
    public Set<String> getLangs() { return langs; }
    public Set<String> getWords() { return words; }
}
