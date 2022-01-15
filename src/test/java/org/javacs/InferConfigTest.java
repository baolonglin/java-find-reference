package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

public class InferConfigTest {
    private Path workspaceRoot = Paths.get("src/test/examples/maven-project");
    private Path mavenHome = Paths.get("src/test/examples/home-dir/.m2");
    private Path gradleHome = Paths.get("src/test/examples/home-dir/.gradle");
    private Set<String> externalDependencies = Set.of("com.external:external-library:1.2");
    private InferConfig both = new InferConfig(workspaceRoot, externalDependencies, mavenHome, gradleHome);
    private InferConfig thisProject = new InferConfig(Paths.get("."), Set.of());

    @Test
    public void mavenClassPath() {
        assertThat(
                both.classPath(),
                contains(mavenHome.resolve("repository/com/external/external-library/1.2/external-library-1.2.jar")));
        // v1.1 should be ignored
    }

    @Test
    public void mavenDocPath() {
        assertThat(
                both.buildDocPath(),
                contains(
                        mavenHome.resolve(
                                "repository/com/external/external-library/1.2/external-library-1.2-sources.jar")));
        // v1.1 should be ignored
    }

    @Test
    public void dependencyList() {
        assertThat(InferConfig.mvnDependencies(Paths.get("pom.xml"), "dependency:list"), not(empty()));
    }

    @Test
    public void thisProjectClassPath() {
        System.out.println(".m2/repository/junit/junit/4.13.2/junit-4.13.2.jar".replace('/', File.separatorChar));
        assertThat(
                thisProject.classPath(),
                hasItem(hasToString(endsWith(".m2/repository/junit/junit/4.13.2/junit-4.13.2.jar".replace('/', File.separatorChar)))));
    }

    @Test
    public void thisProjectDocPath() {
        assertThat(
                thisProject.buildDocPath(),
                hasItem(hasToString(endsWith(".m2/repository/junit/junit/4.13.2/junit-4.13.2-sources.jar".replace('/', File.separatorChar)))));
    }

    @Test
    @Ignore
    public void parseDependencyLine() {
        String[][] testCases = {
            {
                "[INFO]    org.openjdk.jmh:jmh-generator-annprocess:jar:1.21:provided:/Users/georgefraser/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.21/jmh-generator-annprocess-1.21.jar",
                "/Users/georgefraser/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.21/jmh-generator-annprocess-1.21.jar",
            },
            {
                "[INFO]    org.openjdk.jmh:jmh-generator-annprocess:jar:1.21:provided:/Users/georgefraser/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.21/jmh-generator-annprocess-1.21.jar -- module jmh.generator.annprocess (auto)",
                "/Users/georgefraser/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.21/jmh-generator-annprocess-1.21.jar",
            },
        };
        for (var pair : testCases) {
            assert pair.length == 2;
            var line = pair[0];
            var expect = pair[1];
            var path = InferConfig.readDependency(line);
            assertThat(path, equalTo(Paths.get(expect)));
        }
    }
}
