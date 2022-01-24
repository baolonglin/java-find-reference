package org.javacs;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {

    static class Method {
        String name;

        public Method(String name) {
            this.name = name;
        }
    }
    private static final Logger LOG = Logger.getLogger("main");

    public static void main(String[] args) {
        var options = new MainArgs(args);
        if (options.workspace.isEmpty() || options.help) {
            options.printUsage();
            return;
        }

        JavaFindReference finder = new JavaFindReference(Path.of(options.workspace));
        var leafMethods = finder.findLeafReference(options.specialMethods,
                options.depth);
        LOG.info(String.format("Find candidate leaf methods: %d", leafMethods.size()));
        leafMethods.forEach(System.out::println);

        if (leafMethods.isEmpty()) {
            return;
        }

        if (!options.outputJsonFile.isEmpty()) {
            var methods = leafMethods.stream().map(m -> new Method(m)).toArray(Method[]::new);
            try (Writer writer = new FileWriter(options.outputJsonFile)) {
                Gson gson = new GsonBuilder().create();
                gson.toJson(methods, writer);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
