package org.javacs.other;

public class FindLeafHelper {
    public static final int TIMEOUT = 5;

    public void helper1() {
        internalMethod1();
    }

    private void internalMethod1() {
        if(TIMEOUT > 10) {
            internalMethod2();
        } else {
            internalMethod3();
        }
    }
    private void internalMethod2() {}
    private void internalMethod3() {}
}
