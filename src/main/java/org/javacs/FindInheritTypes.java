package org.javacs;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FindInheritTypes extends TreePathScanner<Void, List<String>> {
    final String className;

    String packageName;
    FindInheritTypes(String className) {
        this.className = className;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree root, List<String> found) {
        packageName = Objects.toString(root.getPackageName(), "");
        return super.visitCompilationUnit(root, found);
    }

    @Override
    public Void visitClass(ClassTree type, List<String> found) {
        var parent = type.getExtendsClause();
        if(parent instanceof JCTree.JCIdent) {
            if (((JCTree.JCIdent)parent).sym.getQualifiedName().toString().equals(className)) {
                found.add(packageName + "." + type.getSimpleName());
            }
        }
        return null;
    }
}
