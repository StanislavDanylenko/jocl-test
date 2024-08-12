package stanislav.danylenko;

import java.io.IOException;

public class Main {
    public static final int SIZE = 32;
    public static final int RUN_COUNT = 1000;

    private static final ClassicImpl classic = new ClassicImpl();
    private static final JoclImpl jocl = new JoclImpl();
    private static final JoclImpl2 jocl2 = new JoclImpl2();

    public static void main(String[] args) throws IOException {
        double[] arr1 = getArr(SIZE);
        double[] arr2 = getArr(SIZE);

        jocl.init();
        jocl2.init();

        classic.run(arr1, arr2);
        jocl.run(arr1, arr2);
        jocl2.run(arr1, arr2);

//        classic.multiRun(arr1, arr2);
//        jocl.multiRun(arr1, arr2);
//
//        classic.multiRunParallel(arr1, arr2);
//        jocl.multiRunParallel(arr1, arr2);

        jocl.destroy();
        jocl2.destroy();
    }

    private static double[] getArr(int size) {
        double[] arr = new double[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Math.random();
        }
        return arr;
    }

}