package stanislav.danylenko;

import java.util.stream.IntStream;

public class ClassicImpl {

    public void run(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        int size = arr1.length;
        double[] result = new double[size];

        for (int j = 0; j < size; j++) {
            result[j] = Math.abs(arr1[j] - arr2[j]);
        }

        long finish = System.nanoTime();
        System.out.println("Classic run [single] time: " + (finish - start));
    }

    public void multiRun(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        for (int i = 0; i < Main.RUN_COUNT; i++) {
            int size = arr1.length;
            double[] result = new double[size];

            for (int j = 0; j < size; j++) {
                result[j] = Math.abs(arr1[j] - arr2[j]);
            }
        }

        long finish = System.nanoTime();
        System.out.println("Classic run [multi] time: " + (finish - start));
    }

    public void multiRunParallel(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        IntStream.range(0, Main.RUN_COUNT).parallel().forEach(val -> {
            int size = arr1.length;
            double[] result = new double[size];

            for (int j = 0; j < size; j++) {
                result[j] = Math.abs(arr1[j] - arr2[j]);
            }
        });

        long finish = System.nanoTime();
        System.out.println("Classic run [multi parallel] time: " + (finish - start));
    }

}
