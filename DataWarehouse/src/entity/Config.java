package entity;

import java.sql.Timestamp;

public class Config {
    private int id;
    private String fileName;
    private String author;
    private String email;
    private String directory;
    private String status;
    private String detailPathFile;
    private Timestamp timestamp;
    private int flag;

    public Config(int id, String author, String email, String fileName, String directory, String status, int flag, Timestamp timestamp, String detailPathFile) {
        this.id = id;
        this.author = author;
        this.email = email;
        this.fileName = fileName;
        this.directory = directory;
        this.status = status;
        this.flag = flag;
        this.timestamp = timestamp;
        this.detailPathFile = detailPathFile;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetailPathFile() {
        return detailPathFile;
    }

    public void setDetailPathFile(String detailPathFile) {
        this.detailPathFile = detailPathFile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPath() {
        return directory;
    }

    public void setPath(String path) {
        this.directory = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", author='" + author + '\'' +
                ", email='" + email + '\'' +
                ", directory='" + directory + '\'' +
                ", status='" + status + '\'' +
                ", detailPathFile='" + detailPathFile + '\'' +
                ", timestamp=" + timestamp +
                ", flag=" + flag +
                '}';
    }
}
