package stanislav.danylenko;

public class Main {
    public static final int SIZE = 32;
    public static final int RUN_COUNT = 1000;

    private static final ClassicImpl classic = new ClassicImpl();
    private static final JoclImpl jocl = new JoclImpl();

    public static void main(String[] args) {
        double[] arr1 = getArr(SIZE);
        double[] arr2 = getArr(SIZE);

        jocl.init();

        classic.run(arr1, arr2);
        jocl.run(arr1, arr2);

        classic.multiRun(arr1, arr2);
        jocl.multiRun(arr1, arr2);

        classic.multiRunParallel(arr1, arr2);
        jocl.multiRunParallel(arr1, arr2);

        jocl.destroy();
    }

    private static double[] getArr(int size) {
        double[] arr = new double[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Math.random();
        }
        return arr;
    }

}