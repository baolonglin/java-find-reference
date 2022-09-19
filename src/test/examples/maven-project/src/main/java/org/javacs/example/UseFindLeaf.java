package org.javacs.example;

public class UseFindLeaf {
    public void test1() {
        var f = new FindLeafInheritNoConstructor();
        f.test3();
    }
    public void test2() {
        var f = new FindLeafInherit();
        f.test3();
    }
    public void test3() {
        var f = new FindLeaf();
        f.test3();
    }
}
