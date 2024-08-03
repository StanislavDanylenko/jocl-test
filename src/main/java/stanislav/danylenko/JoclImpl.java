package stanislav.danylenko;

import org.jocl.*;

import java.util.stream.IntStream;

public class JoclImpl {

    /**
     * The source code of the OpenCL program
     */
    private static String programSource =
            "__kernel void "+
                    "sampleKernel(__global const double *a,"+
                    "             __global const double *b,"+
                    "             __global double *c)"+
                    "{"+
                    "    int gid = get_global_id(0);"+
                    "    double res = a[gid] - b[gid];"+
                    "    if (res < 0) res = res * -1;"+
                    "    c[gid] = res;"+
                    "}";

    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;

    public void init() {
        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL.CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int[] numPlatformsArray = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
        CL.clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = CL.clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue =
                CL.clCreateCommandQueue(context, device, 0, null);

        // Create the program from the source code
        program = CL.clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        CL.clBuildProgram(program, 0, null, null, null, null);
    }

    public void destroy() {
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);
    }

    public void run(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        int n = arr1.length;
        double[] result = new double[n];

        Pointer srcA = Pointer.to(arr1);
        Pointer srcB = Pointer.to(arr2);
        Pointer dst = Pointer.to(result);

        // Allocate the memory objects for the input and output data
        cl_mem[] memObjects = new cl_mem[3];
        memObjects[0] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * n, srcA, null);
        memObjects[1] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * n, srcB, null);
        memObjects[2] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_WRITE,
                Sizeof.cl_double * n, null, null);

        cl_kernel kernel = CL.clCreateKernel(program, "sampleKernel", null);

        // Set the arguments for the kernel
        CL.clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        CL.clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));
        CL.clSetKernelArg(kernel, 2,
                Sizeof.cl_mem, Pointer.to(memObjects[2]));

        // Set the work-item dimensions
        long[] global_work_size = new long[]{n};
        long[] local_work_size = new long[]{1};

        // Execute the kernel
        CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        CL.clEnqueueReadBuffer(commandQueue, memObjects[2], CL.CL_TRUE, 0,
                n * Sizeof.cl_double, dst, 0, null, null);

        // Release kernel, program, and memory objects
        CL.clReleaseMemObject(memObjects[0]);
        CL.clReleaseMemObject(memObjects[1]);
        CL.clReleaseMemObject(memObjects[2]);

        long finish = System.nanoTime();
        System.out.println("   Jocl run [single] time: " + (finish - start));
    }

    public void multiRun(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        for (int i = 0; i < Main.RUN_COUNT; i++) {
            int n = arr1.length;
            double[] result = new double[n];

            Pointer srcA = Pointer.to(arr1);
            Pointer srcB = Pointer.to(arr2);
            Pointer dst = Pointer.to(result);

            // Allocate the memory objects for the input and output data
            cl_mem[] memObjects = new cl_mem[3];
            memObjects[0] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_double * n, srcA, null);
            memObjects[1] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_double * n, srcB, null);
            memObjects[2] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_WRITE,
                    Sizeof.cl_double * n, null, null);

            cl_kernel kernel = CL.clCreateKernel(program, "sampleKernel", null);

            // Set the arguments for the kernel
            CL.clSetKernelArg(kernel, 0,
                    Sizeof.cl_mem, Pointer.to(memObjects[0]));
            CL.clSetKernelArg(kernel, 1,
                    Sizeof.cl_mem, Pointer.to(memObjects[1]));
            CL.clSetKernelArg(kernel, 2,
                    Sizeof.cl_mem, Pointer.to(memObjects[2]));

            // Set the work-item dimensions
            long[] global_work_size = new long[]{n};
            long[] local_work_size = new long[]{1};

            // Execute the kernel
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    global_work_size, local_work_size, 0, null, null);

            // Read the output data
            CL.clEnqueueReadBuffer(commandQueue, memObjects[2], CL.CL_TRUE, 0,
                    n * Sizeof.cl_double, dst, 0, null, null);

            // Release kernel, program, and memory objects
            CL.clReleaseMemObject(memObjects[0]);
            CL.clReleaseMemObject(memObjects[1]);
            CL.clReleaseMemObject(memObjects[2]);
        }

        long finish = System.nanoTime();
        System.out.println("   Jocl run [multi] time: " + (finish - start));
    }

    public void multiRunParallel(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        IntStream.range(0, Main.RUN_COUNT).parallel().forEach(val -> {
            int n = arr1.length;
            double[] result = new double[n];

            Pointer srcA = Pointer.to(arr1);
            Pointer srcB = Pointer.to(arr2);
            Pointer dst = Pointer.to(result);

            // Allocate the memory objects for the input and output data
            cl_mem[] memObjects = new cl_mem[3];
            memObjects[0] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_double * n, srcA, null);
            memObjects[1] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_double * n, srcB, null);
            memObjects[2] = CL.clCreateBuffer(context,
                    CL.CL_MEM_READ_WRITE,
                    Sizeof.cl_double * n, null, null);

            cl_kernel kernel = CL.clCreateKernel(program, "sampleKernel", null);

            // Set the arguments for the kernel
            CL.clSetKernelArg(kernel, 0,
                    Sizeof.cl_mem, Pointer.to(memObjects[0]));
            CL.clSetKernelArg(kernel, 1,
                    Sizeof.cl_mem, Pointer.to(memObjects[1]));
            CL.clSetKernelArg(kernel, 2,
                    Sizeof.cl_mem, Pointer.to(memObjects[2]));

            // Set the work-item dimensions
            long[] global_work_size = new long[]{n};
            long[] local_work_size = new long[]{1};

            // Execute the kernel
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    global_work_size, local_work_size, 0, null, null);

            // Read the output data
            CL.clEnqueueReadBuffer(commandQueue, memObjects[2], CL.CL_TRUE, 0,
                    n * Sizeof.cl_double, dst, 0, null, null);

            // Release kernel, program, and memory objects
            CL.clReleaseMemObject(memObjects[0]);
            CL.clReleaseMemObject(memObjects[1]);
            CL.clReleaseMemObject(memObjects[2]);
        });

        long finish = System.nanoTime();
        System.out.println("   Jocl run [multi parallel] time: " + (finish - start));
    }

}
