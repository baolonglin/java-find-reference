package org.javacs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiffParser {
    private Path diffFile;
    private Path workspaceRoot;

    public GitDiffParser(Path workspaceRoot, Path diffFile) {
        this.diffFile = diffFile;
        this.workspaceRoot = workspaceRoot;
    }

    public GitDiffParser(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.diffFile = getGitDiff();
    }

    public GitDiffParser(Path workspaceRoot, String diffStr) {
        this.workspaceRoot = workspaceRoot;
        try {
            this.diffFile = writeTmpFile(diffStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FilePosition> parse() {
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
            while ((strLine = br.readLine()) != null) {
                if (strLine.startsWith("+++")) {
                    toFile = getPath(strLine.substring(5));
                    continue;
                }
                if (JavaFindReference.isJavaFile(toFile)) {
                    continue;
                }
                if (strLine.startsWith("@@")) {
                    if (hunkBodyParseIdx != hunkBodyLineNum) {
                        throw new RuntimeException(
                                "Previous hunk is not parsed success " + strLine);
                    }
                    hunkBody = true;
                    Matcher m = hunkHeaderPattern.matcher(strLine);
                    if (m.matches()) {
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
                        if (!strLine.trim().equals("+")) {
                            int line = hunkBodyStartLine + hunkBodyParseIdx;
                            if (line < 1) {
                                System.err.println(toFile + " has invalid line");
                                line = 1;
                            }
                            modifiedLines.add(new FilePosition(toFile, line, 1));
                        }
                        hunkBodyParseIdx++;
                    } else if (strLine.startsWith("-")) {
                        if (!strLine.trim().equals("-")) {
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
                "git", "diff", "--relative", "HEAD^", "HEAD"
        };
        try {
            var output = Files.createTempFile("git-diff", ".txt");
            var process = new ProcessBuilder().command(command).directory(workspaceRoot.toFile())
                    .redirectError(ProcessBuilder.Redirect.INHERIT).redirectOutput(output.toFile())
                    .start();
            var result = process.waitFor();
            if (result != 0) {
                throw new RuntimeException();
            }

            return output;
        } catch (
                InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path writeTmpFile(String content) throws IOException {
        Path tempFile = Files.createTempFile(null, null);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
            bw.write(content);
        }
        return tempFile;
    }

    private Path getPath(String resourcePath) {
        if (resourcePath.startsWith("/"))
            resourcePath = resourcePath.substring(1);
        return workspaceRoot.resolve(resourcePath).toAbsolutePath().normalize();
    }
}
