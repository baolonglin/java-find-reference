package org.javacs;

import com.sun.source.util.TreePath;

import javax.lang.model.element.TypeElement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReferenceProvider {
    private final CompilerProvider compiler;
    private final Path file;
    private final int line, column;

    public static final List<Location> NOT_SUPPORTED = List.of();

    public ReferenceProvider(CompilerProvider compiler, Path file, int line, int column) {
        this.compiler = compiler;
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public List<String> findImplementations() {
        try (var task = compiler.compile(file)) {
            var element = NavigationHelper.findElement(task, file, line, column);
            if (element != null && NavigationHelper.isType(element)) {
                var type = (TypeElement) element;
                var className = type.getQualifiedName().toString();
                task.close();
                return NavigationHelper.findTypeImplementations(compiler, className);
            }
            return List.of();
        }
    }

    public List<Location> find() {
        try (var task = compiler.compile(file)) {
            var element = NavigationHelper.findElementMethodLevel(task, file, line, column);
            if (element == null) return NOT_SUPPORTED;
            if (NavigationHelper.isLocal(element)) {
                return findReferences(task);
            }
            if (NavigationHelper.isType(element)) {
                var type = (TypeElement) element;
                var className = type.getQualifiedName().toString();
                task.close();
                return findTypeReferences(className);
            }
            if (NavigationHelper.isDefaultConstructor(element)) {
                var parentClass = (TypeElement) element.getEnclosingElement();
                var className = parentClass.getQualifiedName().toString();
                task.close();
                return findDefaultConstructorReferences(className);
            }
            if (NavigationHelper.isMember(element)) {
                var parentClass = (TypeElement) element.getEnclosingElement();
                var className = parentClass.getQualifiedName().toString();
                var memberName = element.getSimpleName().toString();
                if (memberName.equals("<init>")) {
                    memberName = parentClass.getSimpleName().toString();
                }
                task.close();
                return findMemberReferences(className, memberName);
            }
            return NOT_SUPPORTED;
        }
    }

    private List<Location> findDefaultConstructorReferences(String className) {
        var implementations = new ArrayList<String>();
        implementations.add(className);
        NavigationHelper.findTypeAllImplementations(compiler, className, implementations);

        var locations = new ArrayList<Location>();
        for (var implementation : implementations ) {
            var files = compiler.findTypeReferences(implementation);
            if (files.length == 0) {
                continue;
            }
            try (var task = compiler.compile(files)) {
                var paths = new ArrayList<TreePath>();
                for (var root : task.roots) {
                    new FindNewClassWithDefaultConstructor(task.task, implementation).scan(root, paths);
                    new FindAllPublicMethods(implementation).scan(root, paths);
                }
                for (var p : paths) {
                    locations.add(FindHelper.location(task, p));
                }
            }
        }

        return locations;
    }

    private List<Location> findTypeReferences(String className) {
        var files = compiler.findTypeReferences(className);
        if (files.length == 0) return List.of();
        try (var task = compiler.compile(files)) {
            return findReferences(task);
        }
    }

    private List<Location> findMemberReferences(String className, String memberName) {
        var files = compiler.findMemberReferences(className, memberName);
        if (files.length == 0) return List.of();
        try (var task = compiler.compile(files)) {
            return findReferences(task);
        }
    }

    private List<Location> findReferences(CompileTask task) {
        var element = NavigationHelper.findElementMethodLevel(task, file, line, column);
        var paths = new ArrayList<TreePath>();
        for (var root : task.roots) {
            new FindReferences(task.task, element).scan(root, paths);
        }
        var locations = new ArrayList<Location>();
        for (var p : paths) {
            locations.add(FindHelper.location(task, p));
        }
        return locations;
    }
}
