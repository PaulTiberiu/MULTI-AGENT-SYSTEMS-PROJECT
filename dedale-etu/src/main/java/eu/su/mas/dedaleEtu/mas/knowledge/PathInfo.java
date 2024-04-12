package eu.su.mas.dedaleEtu.mas.knowledge;
import java.io.Serializable;
import java.util.List;

public class PathInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> path;

    public PathInfo(List<String> pathToCorner) {
        this.path = pathToCorner;
    }

    // Getters and setters
    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

}