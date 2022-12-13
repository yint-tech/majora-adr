package cn.iinti.majora.adr.majora;

import android.annotation.SuppressLint;
import android.util.Log;

import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.adr.utils.CommonUtils;
import cn.iinti.majora.client.sdk.log.ILogger;
import cn.iinti.majora.client.sdk.log.MajoraLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UILoggerHelper {
    public static void setupLogger() {
        ILogger originLogger = MajoraLogger.getLogger();
        if (originLogger instanceof Logger) {
            originLogger = ((Logger) originLogger).delegate;
        }
        MajoraLogger.setLogger(new Logger(originLogger));
        logConsumeThread.start();
    }

    private static void appendLog(String level, String msg, Throwable throwable) {
        blockingQueue.offer(new LogMessage(level, msg, throwable));
    }

    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static void appendLog2(String level, String msg, Throwable throwable) throws IOException {
        BufferedWriter bufferedWriter = adaptLogWriter();

        bufferedWriter.write(simpleDateFormat.format(new Date()));
        bufferedWriter.write(level);
        bufferedWriter.write(":");
        bufferedWriter.write(msg);
        bufferedWriter.newLine();
        if (throwable != null) {
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
        }
        if (blockingQueue.isEmpty()) {
            bufferedWriter.flush();
        } else {
            if (System.currentTimeMillis() - lastFlushLogTimestamp > 10000) {
                lastFlushLogTimestamp = System.currentTimeMillis();
                bufferedWriter.flush();
            }
        }
    }

    public static void forceCloseWriter() {
        lastLogWriterAdaptTimestamp = 0;
        appendLog(Logger.INFO, "do close logFile", null);
    }

    private static BufferedWriter adaptLogWriter() throws IOException {
        if (System.currentTimeMillis() - lastLogWriterAdaptTimestamp < 120_000 && logWriter != null) {
            return logWriter;
        }
        lastLogWriterAdaptTimestamp = System.currentTimeMillis();
        if (logWriter != null) {
            if (logFile.length() < MAX_LOG_SIZE) {
                return logWriter;
            }
            logWriter.close();
        }
        clearLogFileIfNeed();
        logWriter = new BufferedWriter(new FileWriter(logFile, true));
        return logWriter;
    }

    private static long skipLargeFile(BufferedReader is, long length) throws IOException {
        if (length < MAX_LOG_SIZE)
            return 0;

        long skipped = length - MAX_LOG_SIZE;
        long yetToSkip = skipped;
        do {
            yetToSkip -= is.skip(yetToSkip);
        } while (yetToSkip > 0);

        int c;
        do {
            c = is.read();
            if (c == -1)
                break;
            skipped++;
        } while (c != '\n');

        return skipped;

    }

    private static void clearLogFileIfNeed() throws IOException {
        if (logFile.length() < MAX_LOG_SIZE) {
            return;
        }
        StringBuilder llog = new StringBuilder(15 * 10 * 1024);
        try {
            BufferedReader br;
            br = new BufferedReader(new FileReader(logFile));
            long skipped = skipLargeFile(br, logFile.length());
            if (skipped > 0) {
                llog.append("-----------------\n");
                llog.append("Log too long");
                llog.append("\n-----------------\n\n");
            }

            char[] temp = new char[1024];
            int read;
            while ((read = br.read(temp)) > 0) {
                llog.append(temp, 0, read);
            }
            br.close();
        } catch (IOException e) {
            llog.append("Cannot read log");
            llog.append(e.getMessage());
        }
        CommonUtils.writeFile(logFile, llog.toString());
    }

    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1Mb
    private static long lastFlushLogTimestamp = 0;
    private static long lastLogWriterAdaptTimestamp = 0;
    private static BufferedWriter logWriter = null;

    private static final File logFile = TheApp.getApplication().getLogFile();
    private static final BlockingQueue<LogMessage> blockingQueue = new LinkedBlockingQueue<>();
    private static final Thread logConsumeThread = new Thread("logConsumeThread") {
        @Override
        public void run() {
            while (Thread.currentThread().isAlive()) {
                try {
                    LogMessage message = blockingQueue.take();
                    appendLog2(message.level, message.msg, message.throwable);
                } catch (InterruptedException e) {
                    //ignore
                } catch (IOException e) {
                    Log.e("Majora", "log component error", e);
                }
            }
        }
    };

    private static class LogMessage {
        private final String level;
        private final String msg;
        private final Throwable throwable;

        public LogMessage(String level, String msg, Throwable throwable) {
            this.level = level;
            this.msg = msg;
            this.throwable = throwable;
        }
    }

    public static class Logger implements ILogger {
        private final ILogger delegate;
        private static final String DEBUG = "DEBUG";
        private static final String INFO = "INFO";
        private static final String WARNING = "WARNING";
        private static final String ERROR = "ERROR";

        public Logger(ILogger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void info(String msg) {
            delegate.info(msg);
            appendLog(INFO, msg, null);
        }

        @Override
        public void info(String msg, Throwable throwable) {
            delegate.info(msg, throwable);
            appendLog(INFO, msg, throwable);
        }

        @Override
        public void warn(String msg) {
            delegate.warn(msg);
            appendLog(WARNING, msg, null);
        }

        @Override
        public void warn(String msg, Throwable throwable) {
            delegate.warn(msg, throwable);
            appendLog(WARNING, msg, throwable);
        }

        @Override
        public void error(String msg) {
            delegate.error(msg);
            appendLog(ERROR, msg, null);
        }

        @Override
        public void error(String msg, Throwable throwable) {
            delegate.error(msg, throwable);
            appendLog(ERROR, msg, throwable);
        }

        @Override
        public void debug(String msg) {
            delegate.debug(msg);
            appendLog(DEBUG, msg, null);
        }

        @Override
        public void debug(String msg, Throwable throwable) {
            delegate.debug(msg, throwable);
            appendLog(DEBUG, msg, throwable);
        }
    }
}
