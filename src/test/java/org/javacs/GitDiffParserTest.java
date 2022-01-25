package org.javacs;

import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import org.junit.Test;

public class GitDiffParserTest {

    @Test
    public void parseDiffOneFile() {
        var diffStr = "diff --git a/Main.java b/Main.java\n"
            + "index a954eceb..82039df8 100644\n"
            + "--- a/Main.java\n"
            + "+++ b/Main.java\n"
            + "@@ -27,8 +27,8 @@ public class Main {\n"
            + "             return;\n"
            + "         }\n"
            + "\n"
            + "-        JavaLanguageServer languageServer = new JavaLanguageServer(Path.of(options.workspace));\n"
            + "-        var leafMethods = languageServer.findLeafReference(options.specialMethods,\n"
            + "+        JavaFindReference finder = new JavaFindReference(Path.of(options.workspace));\n"
            + "+        var leafMethods = finder.findLeafReference(options.specialMethods,\n"
            + "                 options.depth);\n"
            + "         LOG.info(String.format(\"Find candidate leaf methods: %d\", leafMethods.size()));\n"
            + "         leafMethods.forEach(System.out::println);\n";
        var changedLines = new GitDiffParser(Paths.get("./src/main/java/org/javacs"), diffStr).parse();
        assertTrue(changedLines.size() == 4);
    }
}
