package org.javacs.example;

import org.javacs.other.FindLeafHelper;

public class FindLeaf {
    private int i = 0;

    void setUp() {
        i = 4;
    }
    void tearDown() {

    }

    public void test1() {
        internal();
    }

    private void internal() {
        deepInternal();
    }

    private void deepInternal() {
        deepDeepInternal();
    }

    private void deepDeepInternal() {
        i = 5;
        var helper = new FindLeafHelper();
        helper.helper1();
    }

    public FindLeaf() {
    }

    public void test2() {}
}
