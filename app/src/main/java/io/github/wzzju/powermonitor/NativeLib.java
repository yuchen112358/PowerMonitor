package io.github.wzzju.powermonitor;

/**
 * Created by yuchen on 16-7-2.
 */

public class NativeLib {
    static {
        System.loadLibrary("NativeLib");
    }

    public static native void Init();

    public static native String GetGPUCurFreq();

    public static native String GetCPUCurFreq(int cpuNum);

    public static native String GetCPUCurLoad(int cpuNum);

    public static native String GetAllCPUCurLoad();

    public static native String GetCPUTemp(int cpuNum);

    public static native int OpenINA231();

    public static native void CloseINA231();

    public static native String GetINA231();
}
