package org.javacs;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import java.util.List;

class FindNewClassWithDefaultConstructor extends TreePathScanner<Void, List<TreePath>> {
    final JavacTask task;
    final String className;
    boolean imported = false;

    FindNewClassWithDefaultConstructor(JavacTask task, String className) {
        this.task = task;
        this.className = className;
    }

    private String getPackageName() {
        return this.className.substring(0, this.className.lastIndexOf("."));
    }

    @Override
    public Void visitPackage(PackageTree t, List<TreePath> list) {
        if (t.getPackageName().toString().equals(getPackageName())) {
            imported = true;
        }
        return super.visitPackage(t, list);
    }

    @Override
    public Void visitImport(ImportTree t, List<TreePath> list) {
        if (t.getQualifiedIdentifier().toString().equals(this.className)) {
            imported = true;
        } else if (t.getQualifiedIdentifier().toString().equals(getPackageName() + ".*")) {
            imported = true;
        }
        return super.visitImport(t, list);
    }

    @Override
    public Void visitNewClass(NewClassTree t, List<TreePath> list) {
        if (t.getArguments().isEmpty() && imported && className.endsWith("." + t.getIdentifier().toString())) {
            list.add(getCurrentPath());
        }

        return super.visitNewClass(t, list);
    }

}
