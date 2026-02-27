package me.lianecx.discordlinker.common.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public final class ConsoleStreamCapture {

    private static final Object LOCK = new Object();

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static boolean installed;

    private ConsoleStreamCapture() {}

    public static void install(Consumer<String> lineConsumer) {
        synchronized(LOCK) {
            if(installed) return;
            originalOut = System.out;
            originalErr = System.err;

            Charset charset = Charset.defaultCharset();
            PrintStream out = new PrintStream(new TeeOutputStream(originalOut, new LineCaptureOutputStream(lineConsumer, charset)), true);
            PrintStream err = new PrintStream(new TeeOutputStream(originalErr, new LineCaptureOutputStream(lineConsumer, charset)), true);

            System.setOut(out);
            System.setErr(err);
            installed = true;
        }
    }

    public static void uninstall() {
        synchronized(LOCK) {
            if(!installed) return;
            System.setOut(originalOut);
            System.setErr(originalErr);
            installed = false;
        }
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream left;
        private final OutputStream right;

        TeeOutputStream(OutputStream left, OutputStream right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void write(int b) throws IOException {
            left.write(b);
            right.write(b);
        }

        @Override
        public void write(byte @NotNull[] b, int off, int len) throws IOException {
            left.write(b, off, len);
            right.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            left.flush();
            right.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }

    private static class LineCaptureOutputStream extends OutputStream {
        private final Consumer<String> lineConsumer;
        private final Charset charset;
        private final StringBuilder lineBuffer = new StringBuilder();

        LineCaptureOutputStream(Consumer<String> lineConsumer, Charset charset) {
            this.lineConsumer = lineConsumer;
            this.charset = charset;
        }

        @Override
        public void write(int b) {
            byte[] single = new byte[] {(byte) b};
            append(new String(single, charset));
        }

        @Override
        public void write(byte @NotNull[] b, int off, int len) {
            if(len <= 0) return;
            append(new String(b, off, len, charset));
        }

        @Override
        public void flush() {
            emitBufferedLine();
        }

        private void append(String content) {
            for(int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if(ch == '\n') {
                    emitBufferedLine();
                    continue;
                }
                if(ch != '\r') lineBuffer.append(ch);
            }
        }

        private void emitBufferedLine() {
            if(lineBuffer.length() == 0) return;
            lineConsumer.accept(lineBuffer.toString());
            lineBuffer.setLength(0);
        }
    }
}
