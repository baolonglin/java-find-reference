package org.javacs;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class NavigationHelper {

    static Element findElement(CompileTask task, Path file, int line, int column) {
        for (var root : task.roots) {
            if (root.getSourceFile().toUri().equals(file.toUri())) {
                var trees = Trees.instance(task.task);
                var cursor = root.getLineMap().getPosition(line, column);
                var path = new FindNameAt(task).scan(root, cursor);
                if (path == null) return null;
                return trees.getElement(path);
            }
        }
        throw new RuntimeException("file not found");
    }

    static Element findMethod(CompileTask task, Path file, int line, int column) {
        for (var root : task.roots) {
            if (root.getSourceFile().toUri().equals(file.toUri())) {
                var trees = Trees.instance(task.task);
                var cursor = root.getLineMap().getPosition(line, column);
                var method = new FindMethodDeclarationAt(task.task).scan(root, cursor);
                if (method == null) {
                    return null;
                }
                var path = trees.getPath(root, method);
                if (path == null) return null;
                return trees.getElement(path);
            }
        }
        throw new RuntimeException("file not found");
    }

    static Element findElementMethodLevel(CompileTask task, Path file, int line, int column) {
        for (var root : task.roots) {
            if (root.getSourceFile().toUri().equals(file.toUri())) {
                var trees = Trees.instance(task.task);
                var cursor = root.getLineMap().getPosition(line, column);
                var method = new FindMethodDeclarationAt(task.task).scan(root, cursor);
                com.sun.source.util.TreePath path;
                if (method == null) {
                    path = new FindNameAt(task).scan(root, cursor);
                } else {
                    path = trees.getPath(root, method);
                }
                if (path == null) return null;
                return trees.getElement(path);
            }
        }
        throw new RuntimeException("file not found");
    }

    public static List<String> findTypeImplementations(CompilerProvider compiler, String className) {
        var files = compiler.findTypeReferences(className);
        if (files.length == 0) return List.of();
        try (var task = compiler.compile(files)) {
            return findImplementations(task, className);
        }
    }

    public static void findTypeAllImplementations(CompilerProvider compiler, String className, List<String> inherits) {
        var inheritClasses = findTypeImplementations(compiler, className);
        for (var c : inheritClasses) {
            if (inherits.contains(c)) {
                continue;
            }
            inherits.add(c);
            findTypeAllImplementations(compiler, c, inherits);
        }
    }

    private static List<String> findImplementations(CompileTask task, String className) {
        var inheritTypes = new ArrayList<String>();
        for (var root : task.roots) {
            new FindInheritTypes(className).scan(root, inheritTypes);
        }
        return inheritTypes;
    }

    static boolean isLocal(Element element) {
        if (element.getModifiers().contains(Modifier.PRIVATE)) {
            return true;
        }
        return switch (element.getKind()) {
            case EXCEPTION_PARAMETER, LOCAL_VARIABLE, PARAMETER, TYPE_PARAMETER -> true;
            default -> false;
        };
    }

    static boolean isMember(Element element) {
        return switch (element.getKind()) {
            case ENUM_CONSTANT, FIELD, METHOD, CONSTRUCTOR -> true;
            default -> false;
        };
    }

    static boolean isDefaultConstructor(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR && element instanceof Symbol.MethodSymbol
                && ((Symbol.MethodSymbol) element).params.isEmpty();
    }


    static boolean isType(Element element) {
        return switch (element.getKind()) {
            case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE -> true;
            default -> false;
        };
    }
}
