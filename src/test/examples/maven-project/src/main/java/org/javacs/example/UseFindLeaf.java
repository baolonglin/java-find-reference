package org.javacs.example;

public class UseFindLeaf {
    public void test1() {
        var f = new FindLeafOverridingDefaultConstructor();
        f.test3();
    }
    public void test2() {
        var f = new FindLeafInheritedDefaultConstructor();
        f.test3();
    }
    public void test3() {
        var f = new FindLeafBase();
        f.test3();
    }
}
