package org.javacs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
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

    private static void findReferences(MainArgs options) {
        JavaFindReference finder = new JavaFindReference(Path.of(options.workspace));

        if (!options.grepOutput.isEmpty()) {
            try (BufferedReader br = new BufferedReader(new FileReader(options.grepOutput))) {
                String line;
                while ((line = br.readLine()) != null) {
                    long startTime = System.nanoTime();
                    var tokens = line.split(":");
                    var position = new FilePosition(new File(tokens[0]).toPath(),
                            Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                    var leafMethods = finder.findLeafReferences(Arrays.asList(position),
                            options.specialMethods, options.depth);
                    handleLeafMethods(leafMethods, (tokens[0] + ":" + tokens[1] + ":" + tokens[2]).replaceAll(File.separator, "-") + ".json");
                    long stopTime = System.nanoTime();
                    LOG.info("== Handling " + line + " takes " + (stopTime - startTime)/1000000000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Set<String> leafMethods = null;
        if (options.position != null) {
            leafMethods = finder.findLeafReferences(Arrays.asList(options.position),
                    options.specialMethods, options.depth);
        } else {
            leafMethods = finder.findLeafReferences(options.specialMethods, options.depth);
        }
        handleLeafMethods(leafMethods, options.outputJsonFile);
    }

    public static void handleLeafMethods(Set<String> leafMethods, String outputJsonFile) {
        LOG.info(String.format("Find candidate leaf methods: %d", leafMethods.size()));
        leafMethods.forEach(System.out::println);

        if (leafMethods.isEmpty()) {
            return;
        }

        if (!outputJsonFile.isEmpty()) {
            var methods = leafMethods.stream().map(m -> new Method(m)).toArray(Method[]::new);
            try (Writer writer = new FileWriter(outputJsonFile)) {
                Gson gson = new GsonBuilder().create();
                gson.toJson(methods, writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        var options = new MainArgs(args);
        if (options.workspace.isEmpty() || options.help) {
            options.printUsage();
            return;
        }

        findReferences(options);
    }
}
