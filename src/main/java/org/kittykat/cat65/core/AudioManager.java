package org.kittykat.cat65.core;

import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.core.expansion.ExpansionManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public abstract class AudioManager {
    public  static final int   READ_BUFFER_SIZE   = 512;
    public  static final int   SAMPLER_BUFFER_SIZE = 4 * READ_BUFFER_SIZE; // probably overkill >w<
    private static final int   AUDIO_BUFFER_SIZE   = 2 * READ_BUFFER_SIZE;
    private static final int   LINE_BUFFER_SIZE    = 2 * AUDIO_BUFFER_SIZE;
    private static final float FILTER_ALPHA = .75f;

    private static final  RingBuffer audioSamplerBuffer = new RingBuffer(SAMPLER_BUFFER_SIZE);
    private static double sampleTimeAccumulator = 0L;
    private static float  filteredSample = 0f;

    @SuppressWarnings({"NonAtomicOperationOnVolatileField", "CallToPrintStackTrace"})
    public static void startAudioThread() {
        Thread audioThread = new Thread(() -> {
            float[] samples;
            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

            int debug_bufferOffset;

            try {
                AudioFormat format = new AudioFormat(Cat65.SAMPLE_RATE, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, LINE_BUFFER_SIZE);
                line.start();

                while (MainManager.running) {
                    int available = audioSamplerBuffer.length();
                    if (available > 0) {
                        samples = audioSamplerBuffer.readBuffer(READ_BUFFER_SIZE);
                    } else {
                        samples = new float[READ_BUFFER_SIZE];
                    }
                    DebugData.audioBufferFillSum += available;
                    DebugData.audioBufferFillCount++;

                    debug_bufferOffset = DebugData.audioBufferSegment * READ_BUFFER_SIZE;
                    for (int s = 0; s < READ_BUFFER_SIZE; s++) {
                        float sample = Math.max(-1f, Math.min(1f, samples[s]));
                        DebugData.audioBuffer[s + debug_bufferOffset] = sample;

                        short pcm = (short) (sample * 0x7fff);
                        buffer[2 * s]     = (byte) (pcm & 0xff);
                        buffer[2 * s + 1] = (byte) ((pcm >> 8) & 0xff);
                    }
                    DebugData.audioBufferSegment = (DebugData.audioBufferSegment + 1) % 2;
                    line.write(buffer, 0, buffer.length);
                }
                line.flush();
                line.close();
            } catch (LineUnavailableException e) {
                System.err.println("[!] Could not start audio thread...");
                e.printStackTrace();
            }
        }, "Cat65 Audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    public static void clockAudio() {
        sampleTimeAccumulator += ConfigManager.clockPeriodProperty.get();
        while (sampleTimeAccumulator >= Cat65.SAMPLE_PERIOD_NANOS) {
            float sample = ExpansionManager.getAudioSample() * (ConfigManager.masterVolumeProperty.get() / 100f);
            filteredSample += FILTER_ALPHA * (sample - filteredSample);

            audioSamplerBuffer.write(filteredSample);
            sampleTimeAccumulator -= Cat65.SAMPLE_PERIOD_NANOS;
        }
    }
}
