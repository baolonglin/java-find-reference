package org.javacs;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFindReference {

    private static final Logger LOG = Logger.getLogger("JavaFindReference");

    private Path workspaceRoot;
    private boolean modifiedBuild = true;
    private JavaCompilerService cacheCompiler;

    JavaCompilerService compiler() {
        if (modifiedBuild) {
            cacheCompiler = createCompiler();
            modifiedBuild = false;
        }
        return cacheCompiler;
    }

    public JavaFindReference(Path rootDir) {
        this.workspaceRoot = rootDir;
        FileStore.setWorkspaceRoots(Set.of(rootDir));
    }

    public static boolean isJavaFile(Path file) {
        var name = file.getFileName().toString();
        return name.endsWith(".java") && !Files.isDirectory(file)
                && !name.equals("module-info.java");
    }

    public Optional<List<String>> findInheritTypes(FilePosition position) {
        if (!isJavaFile(position.path)) {
            return Optional.empty();
        }
        var found = new ReferenceProvider(compiler(), position.path, position.line,
                position.character).findImplementations();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    public Optional<List<Location>> findReferences(FilePosition position) {
        if (!isJavaFile(position.path)) {
            return Optional.empty();
        }
        var found = new ReferenceProvider(compiler(), position.path, position.line,
                position.character).find();
        if (found == ReferenceProvider.NOT_SUPPORTED) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    public String getMethod(FilePosition position) {
        try (var task = compiler().compile(position.path)) {
            var element = NavigationHelper.findMethod(task, position.path, position.line,
                    position.character);
            if (element == null) {
                LOG.info(String.format("Could not find element at (%s:%d:%d)", position.path,
                        position.line, position.character));
                return "";
            }
            if (element.getKind() == ElementKind.METHOD) {
                var parentClass = (TypeElement) element.getEnclosingElement();
                var className = parentClass.getQualifiedName().toString();
                var memberName = element.getSimpleName().toString();
                if (memberName.equals("<init>")) {
                    memberName = parentClass.getSimpleName().toString();
                }
                task.close();
                return className + "::" + memberName;
            } else {
                LOG.info(String.format("Not a method at (%s:%d:%d)", position.path, position.line,
                        position.character));
                return "";
            }
        }
    }

    public Set<String> findLeafReference(List<String> specialMethods, int depth) {
        var leaves = new HashSet<FilePosition>();
        var parsedLocations = new HashSet<Location>();

        var modifiedLines = new GitDiffParser(workspaceRoot).parse();
        var parsedMethod = new HashSet<String>();
        for (var line : modifiedLines) {
            var method = getMethod(line);
            if (method.isEmpty()) {
                continue;
            }
            if (!parsedMethod.contains(method) && method.contains("::")) {
                LOG.info(String.format("Change %s(%d:%d) is owned by %s", line.path.getFileName(),
                        line.line, line.character, method));
                parsedMethod.add(method);
                findReferencesR(line, leaves, parsedLocations, depth);
            } else {
                LOG.info(String.format("Change %s(%d:%d) is parsed before by %s",
                        line.path.getFileName(), line.line, line.character, method));
            }
        }
        var leafMethods = new HashSet<String>();
        for (var l : leaves) {
            var method = getMethod(l);
            if (method.isEmpty()) {
                continue;
            }
            if (specialMethods.stream().anyMatch(e -> method.endsWith(e))) {
                var className = method.substring(0, method.lastIndexOf("::"));
                LOG.info(String.format("Handle special method %s add %s", method, className));
                leafMethods.add(className);
                var inherits = new ArrayList<String>();
                getAllTypeInherits(className, inherits);
                if (!inherits.isEmpty()) {
                    leafMethods.addAll(inherits);
                }
                LOG.info(String.format("Get inherits for %s(%s)", className, inherits));
            } else {
                leafMethods.add(method);
            }
        }
        return leafMethods;
    }

    private void getAllTypeInherits(String className, List<String> inherits) {
        var inheritClasses = NavigationHelper.findTypeImplementations(compiler(), className);
        for (var c : inheritClasses) {
            if (inherits.contains(c)) {
                continue;
            }
            inherits.add(c);
            getAllTypeInherits(c, inherits);
        }
    }

    public Set<FilePosition> findLeafReferences(FilePosition position, int depth) {
        var leaves = new HashSet<FilePosition>();
        var parsedLocations = new HashSet<Location>();
        findReferencesR(position, leaves, parsedLocations, depth);
        return leaves;
    }

    private void findReferencesR(FilePosition position, Set<FilePosition> leaves,
            Set<Location> parsedLocations, int depth) {
        if (depth == 0) {
            return;
        }
        var locations = findReferences(position).orElse(List.of());
        if (locations.isEmpty()) {
            leaves.add(position);
        } else {
            for (Location l : locations) {
                if (parsedLocations.contains(l)) {
                    continue;
                }
                parsedLocations.add(l);
                findReferencesR(new FilePosition(Path.of(l.uri), l.range.start.line + 1,
                        l.range.start.character + 1), leaves, parsedLocations, depth - 1);
            }
        }
    }

    private Set<String> externalDependencies() {
        return Set.of();
    }

    private Set<Path> classPath() {
        return Set.of();
    }

    private Set<String> addExports() {
        return Set.of();
    }

    private JavaCompilerService createCompiler() {
        Objects.requireNonNull(workspaceRoot,
                "Can't create compiler because workspaceRoot has not been initialized");

        var externalDependencies = externalDependencies();
        var classPath = classPath();
        var addExports = addExports();
        // If classpath is specified by the user, don't infer anything
        if (!classPath.isEmpty()) {
            return new JavaCompilerService(classPath, Collections.emptySet(), addExports);
        }
        // Otherwise, combine inference with user-specified external dependencies
        else {
            var infer = new InferConfig(workspaceRoot, externalDependencies);
            classPath = infer.classPath();

            var docPath = infer.buildDocPath();

            return new JavaCompilerService(classPath, docPath, addExports);
        }
    }
}
