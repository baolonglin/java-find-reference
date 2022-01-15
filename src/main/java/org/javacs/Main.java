package org.javacs;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.gson.Gson;

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

        JavaLanguageServer languageServer = new JavaLanguageServer(Path.of(options.workspace));
        var leafMethods = languageServer.findLeafReference(options.specialMethods,
                options.depth);
        LOG.info(String.format("Find candidate leaf methods: %d", leafMethods.size()));
        leafMethods.forEach(LOG::info);

        if (leafMethods.isEmpty()) {
            return;
        }

        if (!options.outputJsonFile.isEmpty()) {
            var methods = leafMethods.stream().map(m -> new Method(m)).toArray(String[]::new);
            Gson gson = new Gson();
            try {            
                gson.toJson(methods, new FileWriter(options.outputJsonFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
