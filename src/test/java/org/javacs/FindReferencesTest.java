package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class FindReferencesTest {
    private static final JavaFindReference finder = new JavaFindReference(
            Paths.get("./src/test/examples/maven-project").toAbsolutePath());

    protected List<String> items(String file, int row, int column) {
        var position = new FilePosition(FindResource.path(file), row, column);
        var locations = finder.findReferences(position).orElse(List.of());
        var strings = new ArrayList<String>();
        for (var l : locations) {
            var fileName = StringSearch.fileName(l.uri);
            var line = l.range.start.line;
            strings.add(String.format("%s(%d)", fileName, line + 1));
        }
        return strings;
    }

    protected Set<String> leaves(String file, int row, int column) {
        var positions = Arrays.asList(new FilePosition(FindResource.path(file), row, column));
        return finder.findLeafReferences(positions, new ArrayList<String>(), -1);
    }

    @Test
    public void findAllReferences() {
        assertThat(items("/main/java/org/javacs/example/GotoOther.java", 6, 30), not(empty()));
    }

    @Test
    public void findInterfaceReference() {
        assertThat(items("/main/java/org/javacs/example/GotoImplementation.java", 9, 21),
                contains("GotoImplementation.java(5)"));
    }

    @Test
    public void findConstructorReferences() {
        assertThat(items("/main/java/org/javacs/example/ConstructorRefs.java", 4, 10),
                contains("ConstructorRefs.java(9)"));
    }

    @Test
    public void referenceIndirectImport() {
        assertThat(items("/main/java/org/javacs/other/ImportIndirectly.java", 4, 25),
                contains("ReferenceIndirectImport.java(9)"));
    }

    @Test
    public void findStackedFieldReferences() {
        var file = "/main/java/org/javacs/example/StackedFieldReferences.java";
        assertThat(items(file, 4, 9), contains("StackedFieldReferences.java(7)"));
        assertThat(items(file, 4, 12), contains("StackedFieldReferences.java(8)"));
        assertThat(items(file, 4, 15), contains("StackedFieldReferences.java(9)"));
    }

    @Test
    public void findLeafReferencesDeep1() {
        var file = "/main/java/org/javacs/example/GotoOther.java";
        assertThat(leaves(file, 4, 26), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 5, 19), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 6, 30), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 7, 17), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 8, 1), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 9, 18), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 10, 14), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 11, 1), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 12, 15), contains("org.javacs.example.Goto::test"));
        assertThat(leaves(file, 13, 1), contains("org.javacs.example.Goto::test"));

        file = "/main/java/org/javacs/example/GotoImplementation.java";
        assertThat(leaves(file, 9, 19), contains("org.javacs.example.GotoImplementation::main"));

        file = "/main/java/org/javacs/example/ConstructorRefs.java";
        assertThat(leaves(file, 4, 10), contains("org.javacs.example.ConstructorRefs::main"));
        assertThat(leaves(file, 6, 10), contains("org.javacs.example.ConstructorRefs::main"));

        file = "/main/java/org/javacs/other/ImportIndirectly.java";
        assertThat(leaves(file, 4, 25), contains("org.javacs.example.ReferenceIndirectImport::test"));

        file = "/main/java/org/javacs/example/StackedFieldReferences.java";
        assertThat(leaves(file, 4, 9), contains("org.javacs.example.StackedFieldReferences::main"));
        assertThat(leaves(file, 4, 12), contains("org.javacs.example.StackedFieldReferences::main"));
        assertThat(leaves(file, 4, 15), contains("org.javacs.example.StackedFieldReferences::main"));

    }

    @Test
    public void findLeafReferences() {
        var file = "/main/java/org/javacs/example/FindLeaf.java";
        assertThat(leaves(file, 24, 13), contains("org.javacs.example.FindLeaf::test1"));
        assertThat(leaves(file, 6, 17),
                containsInAnyOrder("org.javacs.example.FindLeaf::setUp", "org.javacs.example.FindLeaf::test1"));

        file = "/main/java/org/javacs/other/FindLeafHelper.java";
        assertThat(leaves(file, 4, 30), contains("org.javacs.example.FindLeaf::test1"));
    }

    @Test
    public void findLeafDefaultConstructor() {
        var file = "/main/java/org/javacs/example/FindLeaf.java";
        assertThat("leaves mismatch", leaves(file, 33, 14),
                containsInAnyOrder("org.javacs.example.FindLeaf::test2", "org.javacs.example.UseFindLeaf::test1",
                                   "org.javacs.example.UseFindLeaf::test2", "org.javacs.example.UseFindLeaf::test3",
                                   "org.javacs.example.FindLeafInheritNoConstructor::test1", "org.javacs.example.FindLeafInherit::test1",
                                   "org.javacs.example.FindLeaf::test1"
                ));
    }
}
