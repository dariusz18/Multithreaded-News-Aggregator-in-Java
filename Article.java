import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {
    private String uuid;
    private String title;
    private String author;
    private String url;
    private String text;
    private String published;
    private String language;
    private List<String> categories;

    public Article() {
    }

    public Article(String uuid, String title, String author, String url,
                   String text, String published, String language, List<String> categories) {
        this.uuid = uuid;
        this.title = title;
        this.author = author;
        this.url = url;
        this.text = text;
        this.published = published;
        this.language = language;
        this.categories = categories;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getPublished() { return published; }
    public void setPublished(String published) { this.published = published; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}
