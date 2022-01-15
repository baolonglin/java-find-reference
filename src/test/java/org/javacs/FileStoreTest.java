package org.javacs;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class FileStoreTest {

    static Path mavenProjectSrc() {
        return Paths.get("src/test/examples/maven-project").normalize();
    }

    @Before
    public void setWorkspaceRoot() {
        FileStore.setWorkspaceRoots(Set.of(mavenProjectSrc()));
    }

    @Test
    public void packageName() {
        var file = FindResource.path("/org/javacs/example/Goto.java");
        assertThat(FileStore.suggestedPackageName(file), equalTo("org.javacs.example"));
    }
}
