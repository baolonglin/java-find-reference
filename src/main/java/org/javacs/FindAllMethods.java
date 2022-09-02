package org.javacs;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;

import java.util.List;
import java.util.Objects;

public class FindAllMethods extends TreePathScanner<Void, List<Location>> {
    final String className;

    boolean insideClass;
    CompileTask task;
    FindAllMethods(CompileTask task, String className) {
        this.task = task;
        this.className = className;
        insideClass = false;
    }

    @Override
    public Void visitClass(ClassTree type, List<Location> found) {
        if(type.getSimpleName().toString().equals(className)) {
            insideClass = true;
        } else {
        	insideClass = false;
        }
        return super.visitClass(type, found);
    }

    @Override
    public Void visitMethod(MethodTree method, List<Location> found) {
        if(insideClass) {
            found.add(FindHelper.location(task, getCurrentPath()));
        }
        return null;
    }
}
