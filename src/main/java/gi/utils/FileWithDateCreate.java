package gi.utils;

import java.nio.file.Path;
import java.util.Date;

public class FileWithDateCreate implements Comparable {

    private int dirNum;
    private Path path;
    private Date dateCreate;

    public int getDirNum() {
        return dirNum;
    }

    public void setDirNum(int dirNum) {
        this.dirNum = dirNum;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Date getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(Date dateCreate) {
        this.dateCreate = dateCreate;
    }

    @Override
    public int compareTo(Object o) {
        FileWithDateCreate m = (FileWithDateCreate) o;

        // сначала сравнение по dirNum
        // затем по dateCreate

        if (this.dirNum > m.dirNum) {
            return 1;
        } else if (this.dirNum < m.dirNum) {
            return -1;
        } else {
            return this.dateCreate.compareTo(m.dateCreate);
        }
    }
}

