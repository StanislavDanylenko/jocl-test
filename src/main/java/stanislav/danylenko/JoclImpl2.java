package stanislav.danylenko;

import com.jogamp.opencl.*;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.stream.IntStream;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static java.lang.System.out;

public class JoclImpl2 {

    CLContext context;
    int globalWorkSize;
    CLCommandQueue queue;
    CLProgram program;
    int elementCount = 32;
    int localWorkSize;

    public void init() throws IOException {
        // set up (uses default CLPlatform and creates context for all devices)
        context = CLContext.create();
        out.println("created "+context);

        // select fastest device
        CLDevice device = context.getMaxFlopsDevice();
        out.println("using "+device);

        // create command queue on device.
        queue = device.createCommandQueue();

                                   // Length of arrays to process
        localWorkSize = min(device.getMaxWorkGroupSize(), 256);  // Local work size dimensions
        globalWorkSize = roundUp(localWorkSize, elementCount);   // rounded up to the nearest multiple of the localWorkSize

        // load sources, create and build program
        program = context.createProgram(JoclImpl2.class.getResourceAsStream("/VectorAdd.cl")).build();
    }

    public void destroy() {
        // cleanup all resources associated with this context.
        context.release();
    }

    public void run(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        // always make sure to release the context under all circumstances
        // not needed for this particular sample but recommented
            // A, B are input buffers, C is for the result
            CLBuffer<DoubleBuffer> clBufferA = context.createDoubleBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<DoubleBuffer> clBufferB = context.createDoubleBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<DoubleBuffer> clBufferC = context.createDoubleBuffer(globalWorkSize, WRITE_ONLY);

            out.println("used device memory: "
                    + (clBufferA.getCLSize()+clBufferB.getCLSize()+clBufferC.getCLSize())/1000000 +"MB");


            // fill input buffers with random numbers
            // (just to have test data; seed is fixed -> results will not change between runs).
            fillBuffer(clBufferA.getBuffer(), arr1);
            fillBuffer(clBufferB.getBuffer(), arr2);

            // get a reference to the kernel function with the name 'VectorAdd'
            // and map the buffers to its input parameters.
            CLKernel kernel = program.createCLKernel("VectorAdd");
            kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);

            // asynchronous write of data to GPU device,
            // followed by blocking read to get the computed results back.
            long time = nanoTime();
            queue.putWriteBuffer(clBufferA, false)
                    .putWriteBuffer(clBufferB, false)
                    .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
                    .putReadBuffer(clBufferC, true);
            time = nanoTime() - time;

            // print first few elements of the resulting buffer to the console.
            out.println("a+b=c results snapshot: ");
            for(int i = 0; i < 10; i++)
                out.print(clBufferC.getBuffer().get() + ", ");
            out.println("...; " + clBufferC.getBuffer().remaining() + " more");

            out.println("computation took: "+(time/1000000)+"ms");


        long finish = System.nanoTime();
        System.out.println("Jocl2 run [single] time: " + (finish - start));
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

    public static void main(String[] args) throws IOException {



    }

    private static void fillBuffer(DoubleBuffer buffer, double[] arr) {
        for (double v : arr) {
            buffer.put(v);
        }
        buffer.rewind();
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }

}
