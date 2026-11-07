package ar.didgetaller.engineering;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

/** Controlled offline-DSP samples, not an Android audio/recomposition benchmark. */
public final class DspBenchmark {
    private DspBenchmark() { }

    public static void main(String[] arguments) throws Exception {
        Class<?> detector = Class.forName("ar.didgetaller.core.PitchDetector");
        Object instance = detector.getField("INSTANCE").get(null);
        Method analyze = detector.getMethod("analyze", short[].class, int.class, int.class, int.class, boolean.class);
        Method frequency = Class.forName("ar.didgetaller.core.PitchDetector$Reading").getMethod("getHz");
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        boolean memory = bean.isThreadAllocatedMemorySupported();
        if (memory) bean.setThreadAllocatedMemoryEnabled(true);
        for (String name : new String[] {"DIDGERIDOO", "NATIVE_AMERICAN_FLUTE"}) {
            Class<?> profiles = Class.forName("ar.didgetaller.core.InstrumentProfile");
            Object profile = profiles.getField(name).get(null);
            int size = (int) profiles.getMethod("getWindowSamples").invoke(profile);
            int low = (int) profiles.getMethod("getMinHz").invoke(profile);
            int high = (int) profiles.getMethod("getMaxHz").invoke(profile);
            double hz = name.equals("DIDGERIDOO") ? 70 : 440;
            short[] pcm = new short[size];
            for (int i = 0; i < size; i++) pcm[i] = (short) (int) (8000 * Math.sin(2 * Math.PI * hz * i / 44100));
            double checksum = 0;
            for (int i = 0; i < 120; i++) checksum += frame(analyze, frequency, instance, pcm, low, high);
            var times = new ArrayList<Double>();
            var bytes = new ArrayList<Double>();
            for (int sample = 0; sample < 5; sample++) {
                long before = memory ? bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
                long start = System.nanoTime();
                for (int i = 0; i < 50; i++) checksum += frame(analyze, frequency, instance, pcm, low, high);
                times.add((System.nanoTime() - start) / 50.0);
                if (memory) bytes.add((bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before) / 50.0);
            }
            var ordered = new ArrayList<>(times);
            Collections.sort(ordered);
            Collections.sort(bytes);
            System.out.println(name + " median_ns_per_frame=" + ordered.get(2) + " median_bytes_per_frame="
                    + (memory ? bytes.get(2) : "unavailable") + " samples_ns=" + times + " checksum=" + checksum);
        }
    }

    private static double frame(Method analyze, Method frequency, Object instance, short[] pcm, int low, int high) throws Exception {
        return (double) frequency.invoke(analyze.invoke(instance, pcm, 44100, low, high, false));
    }
}
