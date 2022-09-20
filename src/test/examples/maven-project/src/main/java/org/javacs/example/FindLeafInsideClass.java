package org.javacs.example;

public class FindLeafInsideClass {
    private int i = 0;

    public void test1() {
        internal();
    }

    private void internal() {
        deepInternal();
    }

    private void deepInternal() {
        i = 4;
    }

    public void test2() {
        i = 5;
    }

    public FindLeafInsideClass() {

    }
}
