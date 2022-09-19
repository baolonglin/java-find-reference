package org.javacs;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import javax.lang.model.element.Modifier;
import com.sun.source.tree.PackageTree;

import java.util.List;

public class FindAllPublicMethods extends TreePathScanner<Void, List<TreePath>> {
    final String className;

    boolean insideClass;
    String currentPackage;

    FindAllPublicMethods(String className) {
        this.className = className;
        insideClass = false;
    }

    @Override
    public Void visitPackage(PackageTree t, List<TreePath> found) {
        currentPackage = t.getPackageName().toString();
        return super.visitPackage(t, found);
    }

    @Override
    public Void visitClass(ClassTree type, List<TreePath> found) {
        if((currentPackage + "." + type.getSimpleName().toString()).equals(className)) {
            insideClass = true;
        } else {
        	insideClass = false;
        }
        return super.visitClass(type, found);
    }

    @Override
    public Void visitMethod(MethodTree method, List<TreePath> found) {
        if(insideClass && method.getReturnType() != null && method.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
            found.add(getCurrentPath());
        }
        return null;
    }
}
