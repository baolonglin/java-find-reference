package org.javacs.example;

public class Goto<Param> {
    public void test() {
        Object local;
        Runnable reference;
        GotoOther other;

        // References to this class
        local = new Goto("Foo");

        local = Goto.fieldStatic;
        local = this.field;

        Goto.methodStatic();
        this.method();

        reference = Goto::methodStatic;
        reference = this::method;

        // References to other class on sourcepath
        other = new GotoOther("Foo");

        local = GotoOther.fieldStatic;
        local = other.field;

        GotoOther.methodStatic();
        other.method();

        reference = GotoOther::methodStatic;
    }

    public static String fieldStatic;
    public String field;
    public static String methodStatic() {
        return "foo";
    }
    public String method() {
        return "foo";
    }
    public Goto(String nonDefaultConstructor) {
    }

    void testGotoPackagePrivate() {
        var x = GotoPackagePrivate.PUBLIC_FIELD;
    }
}
