package stanislav.danylenko;

import org.jocl.*;

import java.util.stream.IntStream;

public class JoclImpl {

    private static final String programSource =
            "__kernel void sampleKernel(__global const double *a, " +
                    "                          __global const double *b, " +
                    "                          __global double *c) { " +
                    "    int gid = get_global_id(0); " +
                    "    double res = fabs(a[gid] - b[gid]); " +
                    "    c[gid] = res; " +
                    "}";

    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;

    public void init() {
        final int platformIndex = 0;
        final long deviceType = CL.CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        CL.setExceptionsEnabled(true);

        cl_platform_id platform = getPlatform(platformIndex);
        cl_device_id device = getDevice(platform, deviceType, deviceIndex);

        context = CL.clCreateContext(
                createContextProperties(platform),
                1, new cl_device_id[]{device}, null, null, null);

        commandQueue = CL.clCreateCommandQueue(context, device, 0, null);

        program = CL.clCreateProgramWithSource(context, 1, new String[]{ programSource }, null, null);
        CL.clBuildProgram(program, 0, null, null, null, null);
    }

    private cl_platform_id getPlatform(int platformIndex) {
        int[] numPlatformsArray = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatformsArray);
        cl_platform_id[] platforms = new cl_platform_id[numPlatformsArray[0]];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        return platforms[platformIndex];
    }

    private cl_device_id getDevice(cl_platform_id platform, long deviceType, int deviceIndex) {
        int[] numDevicesArray = new int[1];
        CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        cl_device_id[] devices = new cl_device_id[numDevicesArray[0]];
        CL.clGetDeviceIDs(platform, deviceType, numDevicesArray[0], devices, null);
        return devices[deviceIndex];
    }

    private cl_context_properties createContextProperties(cl_platform_id platform) {
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);
        return contextProperties;
    }

    public void destroy() {
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);
    }

    public void run(double[] arr1, double[] arr2) {
        long start = System.nanoTime();

        double[] result = new double[arr1.length];
        executeKernel(arr1, arr2, result);

        long finish = System.nanoTime();
        System.out.println("   Jocl run [single] time: " + (finish - start));
    }

    private void executeKernel(double[] arr1, double[] arr2, double[] result) {
        Pointer srcA = Pointer.to(arr1);
        Pointer srcB = Pointer.to(arr2);
        Pointer dst = Pointer.to(result);

        cl_mem[] memObjects = createMemObjects(srcA, srcB, arr1.length);

        cl_kernel kernel = CL.clCreateKernel(program, "sampleKernel", null);
        setKernelArgs(kernel, memObjects);

        long[] global_work_size = new long[]{arr1.length};
        long[] local_work_size = new long[]{1};

        CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        CL.clEnqueueReadBuffer(commandQueue, memObjects[2], CL.CL_TRUE, 0,
                arr1.length * Sizeof.cl_double, dst, 0, null, null);

        releaseMemObjects(memObjects);
        CL.clReleaseKernel(kernel);
    }

    private cl_mem[] createMemObjects(Pointer srcA, Pointer srcB, int n) {
        cl_mem[] memObjects = new cl_mem[3];
        memObjects[0] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * n, srcA, null);
        memObjects[1] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_double * n, srcB, null);
        memObjects[2] = CL.clCreateBuffer(context,
                CL.CL_MEM_READ_WRITE, Sizeof.cl_double * n, null, null);
        return memObjects;
    }

    private void setKernelArgs(cl_kernel kernel, cl_mem[] memObjects) {
        CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
    }

    private void releaseMemObjects(cl_mem[] memObjects) {
        for (cl_mem mem : memObjects) {
            CL.clReleaseMemObject(mem);
        }
    }

    public void multiRun(double[] arr1, double[] arr2) {
        long start = System.nanoTime();
        for (int i = 0; i < Main.RUN_COUNT; i++) {
            double[] result = new double[arr1.length];
            executeKernel(arr1, arr2, result);
        }
        long finish = System.nanoTime();
        System.out.println("   Jocl run [multi] time: " + (finish - start));
    }

    public void multiRunParallel(double[] arr1, double[] arr2) {
        long start = System.nanoTime();
        IntStream.range(0, Main.RUN_COUNT).parallel().forEach(val -> {
            double[] result = new double[arr1.length];
            executeKernel(arr1, arr2, result);
        });
        long finish = System.nanoTime();
        System.out.println("   Jocl run [multi parallel] time: " + (finish - start));
    }
}

