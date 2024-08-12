package stanislav.danylenko;

import com.jogamp.opencl.*;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static java.lang.System.out;

/**
 * Hello Java OpenCL example. Adds all elements of buffer A to buffer B
 * and stores the result in buffer C.<br/>
 * Sample was inspired by the Nvidia VectorAdd example written in C/C++
 * which is bundled in the Nvidia OpenCL SDK.
 * @author Michael Bien
 */
public class HelloJOCL {

    public static void main(String[] args) throws IOException {

        // set up (uses default CLPlatform and creates context for all devices)
        CLContext context = CLContext.create();
        out.println("created "+context);

        // always make sure to release the context under all circumstances
        // not needed for this particular sample but recommented
        try{
            for (int j = 0; j < 1000; j++) {
            // select fastest device
            CLDevice device = context.getMaxFlopsDevice();
            out.println("using "+device);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            int elementCount = 32;                                  // Length of arrays to process
            int localWorkSize = min(device.getMaxWorkGroupSize(), 256);  // Local work size dimensions
            int globalWorkSize = roundUp(localWorkSize, elementCount);   // rounded up to the nearest multiple of the localWorkSize

            // load sources, create and build program
            CLProgram program = context.createProgram(HelloJOCL.class.getResourceAsStream("/VectorAdd.cl")).build();

            // A, B are input buffers, C is for the result
            CLBuffer<DoubleBuffer> clBufferA = context.createDoubleBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<DoubleBuffer> clBufferB = context.createDoubleBuffer(globalWorkSize, READ_ONLY);
            CLBuffer<DoubleBuffer> clBufferC = context.createDoubleBuffer(globalWorkSize, WRITE_ONLY);

            out.println("used device memory: "
                    + (clBufferA.getCLSize()+clBufferB.getCLSize()+clBufferC.getCLSize())/1000000 +"MB");


            // fill input buffers with random numbers
            // (just to have test data; seed is fixed -> results will not change between runs).
            fillBuffer(clBufferA.getBuffer(), 12345);
            fillBuffer(clBufferB.getBuffer(), 67890);

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
            }
        }finally{
            // cleanup all resources associated with this context.
            context.release();
        }

    }

    private static void fillBuffer(DoubleBuffer buffer, int seed) {
        Random rnd = new Random(seed);
        while(buffer.remaining() != 0)
            buffer.put(rnd.nextDouble()*100);
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
