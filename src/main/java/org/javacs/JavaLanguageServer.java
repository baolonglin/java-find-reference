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

public class JavaLanguageServer {

    private static final Logger LOG = Logger.getLogger("JavaLanguageServer");

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

    public JavaLanguageServer(Path rootDir) {
        this.workspaceRoot = rootDir;
        FileStore.setWorkspaceRoots(Set.of(rootDir));
    }

    public Path getPath(String resourcePath) {
        if(resourcePath.startsWith("/")) resourcePath = resourcePath.substring(1);
        return this.workspaceRoot.resolve(resourcePath).toAbsolutePath().normalize();
    }

    private static boolean isJavaFile(Path file) {
        var name = file.getFileName().toString();
        return name.endsWith(".java") && !Files.isDirectory(file) && !name.equals("module-info.java");
    }

    private List<FilePosition> getGitModifiedLines() {
        var diffFile = getGitDiff();
        return parseDiffFile(diffFile);
    }

    private List<FilePosition> parseDiffFile(Path diffFile) {
        var modifiedLines = new ArrayList<FilePosition>();
        Pattern hunkHeaderPattern = Pattern.compile("^@@ -[0-9,]+ \\+(\\d+)(?:,(\\d+))? @@.*$");
        try {
            FileInputStream fstream = new FileInputStream(diffFile.toFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            Path toFile = null;
            boolean hunkBody = false;
            int hunkBodyStartLine = 0;
            int hunkBodyLineNum = 0;
            int hunkBodyParseIdx = 0;
            while ((strLine = br.readLine()) != null)   {
                if(strLine.startsWith("+++")) {
                    toFile = getPath(strLine.substring(5));
                    continue;
                }
                if (strLine.startsWith("@@")) {
                    if (hunkBodyParseIdx != hunkBodyLineNum) {
                        throw new RuntimeException("Previous hunk is not parsed success " + strLine);
                    }
                    hunkBody = true;
                    Matcher m = hunkHeaderPattern.matcher(strLine);
                    if(m.matches()) {
                        hunkBodyStartLine = Integer.parseInt(m.group(1));
                        hunkBodyLineNum = 1;
                        if (m.group(2) != null) {
                            hunkBodyLineNum = Integer.parseInt(m.group(2));
                        }
                    } else {
                        throw new RuntimeException("Invalid hunk header");
                    }
                    hunkBodyParseIdx = 0;
                    continue;
                }
                if (hunkBody && hunkBodyParseIdx < hunkBodyLineNum) {
                    if (strLine.startsWith("+")) {
                        if(isJavaFile(toFile)) {
                            int line = hunkBodyStartLine + hunkBodyParseIdx;
                            if (line < 1) {
                                System.err.println(toFile + " has invalid line");
                                line = 1;
                            }
                            modifiedLines.add(new FilePosition(toFile, line, 1));
                        }
                        hunkBodyParseIdx++;
                    } else if(strLine.startsWith("-")) {
                        if(isJavaFile(toFile)) {
                            int line = hunkBodyStartLine + hunkBodyParseIdx - 1;
                            if (line < 1) {
                                System.err.println(toFile + " has invalid line");
                                line = 1;
                            }
                            modifiedLines.add(new FilePosition(toFile, line, 1));
                        }
                    } else {
                        hunkBodyParseIdx++;
                    }
                }
            }
            fstream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return modifiedLines;
    }

    private Path getGitDiff() {
        String[] command = {
                "git",
                "diff",
                "--relative",
                "HEAD^",
                "HEAD"
        };
        try {
            var output = Files.createTempFile("git-diff", ".txt");
            var process = new ProcessBuilder().command(command).directory(workspaceRoot.toFile())
                    .redirectError(ProcessBuilder.Redirect.INHERIT).redirectOutput(output.toFile()).start();
            var result = process.waitFor();
            if (result != 0) {
                throw new RuntimeException();
            }

            return output;
        }
        catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<List<String>> findInheritTypes(FilePosition position) {
        if (!isJavaFile(position.path)) {
            return Optional.empty();
        }
        var found = new ReferenceProvider(compiler(), position.path, position.line, position.character).findImplementations();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    public Optional<List<Location>> findReferences(FilePosition position) {
        if (!isJavaFile(position.path)) {
            return Optional.empty();
        }
        var found = new ReferenceProvider(compiler(), position.path, position.line, position.character).find();
        if (found == ReferenceProvider.NOT_SUPPORTED) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    public String getMethod(FilePosition position) {
        try (var task = compiler().compile(position.path)) {
            var element = NavigationHelper.findMethod(task, position.path, position.line, position.character);
            if (element == null) {
                return "Not Found";
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
                return "Not Method";
            }
        }
    }

    public Set<String> findLeafReference(List<String> specialMethods, int depth) {
        var leaves = new HashSet<FilePosition>();
        var parsedLocations = new HashSet<Location>();

        var modifiedLines = getGitModifiedLines();
        var parsedMethod = new HashSet<String>();
        for (var line : modifiedLines) {
            var method = getMethod(line);
            if(!parsedMethod.contains(method) && method.contains("::")) {
                LOG.info(String.format("Change %s(%d:%d) is owned by %s", line.path.getFileName(), line.line, line.character, method));
                parsedMethod.add(method);
                findReferencesR(line, leaves, parsedLocations, depth);
            } else {
                LOG.info(String.format("Change %s(%d:%d) is parsed before by %s", line.path.getFileName(), line.line, line.character, method));
            }
        }
        var leafMethods = new HashSet<String>();
        for(var l : leaves) {
            var method = getMethod(l);
            if (specialMethods.stream().anyMatch(e -> method.endsWith(e))) {
                var className = method.substring(0, method.lastIndexOf("::"));
                LOG.info(String.format("Handle special method %s add %s", method, className));
                leafMethods.add(className);
                var inherits = new ArrayList<String>();
                getAllTypeInherits(className, inherits);
                if(!inherits.isEmpty()) {
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
        for(var c : inheritClasses) {
            if(inherits.contains(c)) {
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

    private void findReferencesR(FilePosition position, Set<FilePosition> leaves, Set<Location> parsedLocations, int depth) {
        if(depth == 0) {
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
                findReferencesR(new FilePosition(Path.of(l.uri), l.range.start.line + 1, l.range.start.character + 1), leaves, parsedLocations, depth - 1);
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
        Objects.requireNonNull(workspaceRoot, "Can't create compiler because workspaceRoot has not been initialized");

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
