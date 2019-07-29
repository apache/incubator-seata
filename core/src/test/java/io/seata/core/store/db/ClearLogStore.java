package io.seata.core.store.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClearLogStore {

    public static void clearTempLogStore(String store) {
        Path path = Paths.get(store);

        boolean isDirectory = Files.isDirectory(path);

        if (isDirectory) {
            try {
                Files.list(path).forEach(subPath -> {
                    try {
                        Files.delete(subPath);
                    } catch (IOException e) {
                    }
                });
                Files.delete(path);
            } catch (IOException e) {
            }
        }
    }
}
