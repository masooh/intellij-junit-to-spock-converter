package sample;

public class Book {
    private String title;
    private Integer pages;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        if (pages < 1) {
            throw new IllegalArgumentException("pages must be greater 0");
        }
        this.pages = pages;
    }

    public String prefixTitle(String prefix) {
        title = prefix + " " + title;
        return title;
    }
}
