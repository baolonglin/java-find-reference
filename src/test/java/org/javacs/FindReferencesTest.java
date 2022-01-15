package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class FindReferencesTest {
    private static final JavaLanguageServer server = new JavaLanguageServer(Paths.get("./src/test/examples/maven-project").toAbsolutePath());

    protected List<String> items(String file, int row, int column) {
        var position = new FilePosition(FindResource.path(file), row, column);
        var locations = server.findReferences(position).orElse(List.of());
        var strings = new ArrayList<String>();
        for (var l : locations) {
            var fileName = StringSearch.fileName(l.uri);
            var line = l.range.start.line;
            strings.add(String.format("%s(%d)", fileName, line + 1));
        }
        return strings;
    }

    @Test
    public void findAllReferences() {
        assertThat(items("/org/javacs/example/GotoOther.java", 6, 30), not(empty()));
    }

    @Test
    public void findInterfaceReference() {
        assertThat(items("/org/javacs/example/GotoImplementation.java", 9, 21), contains("GotoImplementation.java(5)"));
    }

    @Test
    public void findConstructorReferences() {
        assertThat(items("/org/javacs/example/ConstructorRefs.java", 4, 10), contains("ConstructorRefs.java(9)"));
    }

    @Test
    public void referenceIndirectImport() {
        assertThat(
                items("/org/javacs/other/ImportIndirectly.java", 4, 25), contains("ReferenceIndirectImport.java(9)"));
    }

    @Test
    public void findStackedFieldReferences() {
        var file = "/org/javacs/example/StackedFieldReferences.java";
        assertThat(items(file, 4, 9), contains("StackedFieldReferences.java(7)"));
        assertThat(items(file, 4, 12), contains("StackedFieldReferences.java(8)"));
        assertThat(items(file, 4, 15), contains("StackedFieldReferences.java(9)"));
    }
}
