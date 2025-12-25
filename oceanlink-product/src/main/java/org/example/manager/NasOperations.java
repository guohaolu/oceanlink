package org.example.manager;

import java.io.InputStream;
import java.util.List;

/**
 * @author guohao.lu
 */
public interface NasOperations {
    List<String> list(String path);

    boolean exists(String path);

    void delete(String path);

    void mkdirs(String path);

    InputStream openStream(String path);
}
