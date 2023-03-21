package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import net.minecraft.util.MemoryReserve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

public class CrashReport {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private final String title;
    private final Throwable exception;
    private final List<CrashReportSystemDetails> details = Lists.newArrayList();
    private File saveFile;
    private boolean trackingStackTrace = true;
    private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];
    private final SystemReport systemReport = new SystemReport();

    public CrashReport(String s, Throwable throwable) {
        this.title = s;
        this.exception = throwable;
    }

    public String getTitle() {
        return this.title;
    }

    public Throwable getException() {
        return this.exception;
    }

    public String getDetails() {
        StringBuilder stringbuilder = new StringBuilder();

        this.getDetails(stringbuilder);
        return stringbuilder.toString();
    }

    public void getDetails(StringBuilder stringbuilder) {
        if ((this.uncategorizedStackTrace == null || this.uncategorizedStackTrace.length <= 0) && !this.details.isEmpty()) {
            this.uncategorizedStackTrace = (StackTraceElement[]) ArrayUtils.subarray(((CrashReportSystemDetails) this.details.get(0)).getStacktrace(), 0, 1);
        }

        if (this.uncategorizedStackTrace != null && this.uncategorizedStackTrace.length > 0) {
            stringbuilder.append("-- Head --\n");
            stringbuilder.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
            stringbuilder.append("Stacktrace:\n");
            StackTraceElement[] astacktraceelement = this.uncategorizedStackTrace;
            int i = astacktraceelement.length;

            for (int j = 0; j < i; ++j) {
                StackTraceElement stacktraceelement = astacktraceelement[j];

                stringbuilder.append("\t").append("at ").append(stacktraceelement);
                stringbuilder.append("\n");
            }

            stringbuilder.append("\n");
        }

        Iterator iterator = this.details.iterator();

        while (iterator.hasNext()) {
            CrashReportSystemDetails crashreportsystemdetails = (CrashReportSystemDetails) iterator.next();

            crashreportsystemdetails.getDetails(stringbuilder);
            stringbuilder.append("\n\n");
        }

        this.systemReport.appendToCrashReportString(stringbuilder);
    }

    public String getExceptionMessage() {
        StringWriter stringwriter = null;
        PrintWriter printwriter = null;
        Object object = this.exception;

        if (((Throwable) object).getMessage() == null) {
            if (object instanceof NullPointerException) {
                object = new NullPointerException(this.title);
            } else if (object instanceof StackOverflowError) {
                object = new StackOverflowError(this.title);
            } else if (object instanceof OutOfMemoryError) {
                object = new OutOfMemoryError(this.title);
            }

            ((Throwable) object).setStackTrace(this.exception.getStackTrace());
        }

        String s;

        try {
            stringwriter = new StringWriter();
            printwriter = new PrintWriter(stringwriter);
            ((Throwable) object).printStackTrace(printwriter);
            s = stringwriter.toString();
        } finally {
            IOUtils.closeQuietly(stringwriter);
            IOUtils.closeQuietly(printwriter);
        }

        return s;
    }

    public String getFriendlyReport() {
        StringBuilder stringbuilder = new StringBuilder();

        stringbuilder.append("---- 서버 크래쉬 보고서 ----\n");
        stringbuilder.append("// ");
        stringbuilder.append(getErrorComment());
        stringbuilder.append("\n\n");
        stringbuilder.append("시간: ");
        stringbuilder.append(CrashReport.DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
        stringbuilder.append("\n");
        stringbuilder.append("설명: ");
        stringbuilder.append(this.title);
        stringbuilder.append("\n\n");
        stringbuilder.append(this.getExceptionMessage());
        stringbuilder.append("\n\n이 오류와, 코드 위치 그리고, 모든 자세한 정보는 아래에 따릅니다.\n");

        for (int i = 0; i < 87; ++i) {
            stringbuilder.append("-");
        }

        stringbuilder.append("\n\n");
        this.getDetails(stringbuilder);
        return stringbuilder.toString();
    }

    public File getSaveFile() {
        return this.saveFile;
    }

    public boolean saveToFile(File file) {
        if (this.saveFile != null) {
            return false;
        } else {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            OutputStreamWriter outputstreamwriter = null;

            boolean flag;

            try {
                outputstreamwriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                outputstreamwriter.write(this.getFriendlyReport());
                this.saveFile = file;
                boolean flag1 = true;

                return flag1;
            } catch (Throwable throwable) {
                CrashReport.LOGGER.error("{}에 저장하는데 오류가 발생함.", file, throwable);
                flag = false;
            } finally {
                IOUtils.closeQuietly(outputstreamwriter);
            }

            return flag;
        }
    }

    public SystemReport getSystemReport() {
        return this.systemReport;
    }

    public CrashReportSystemDetails addCategory(String s) {
        return this.addCategory(s, 1);
    }

    public CrashReportSystemDetails addCategory(String s, int i) {
        CrashReportSystemDetails crashreportsystemdetails = new CrashReportSystemDetails(s);

        if (this.trackingStackTrace) {
            int j = crashreportsystemdetails.fillInStackTrace(i);
            StackTraceElement[] astacktraceelement = this.exception.getStackTrace();
            StackTraceElement stacktraceelement = null;
            StackTraceElement stacktraceelement1 = null;
            int k = astacktraceelement.length - j;

            if (k < 0) {
                System.out.println("Negative index in crash report handler (" + astacktraceelement.length + "/" + j + ")");
            }

            if (astacktraceelement != null && 0 <= k && k < astacktraceelement.length) {
                stacktraceelement = astacktraceelement[k];
                if (astacktraceelement.length + 1 - j < astacktraceelement.length) {
                    stacktraceelement1 = astacktraceelement[astacktraceelement.length + 1 - j];
                }
            }

            this.trackingStackTrace = crashreportsystemdetails.validateStackTrace(stacktraceelement, stacktraceelement1);
            if (astacktraceelement != null && astacktraceelement.length >= j && 0 <= k && k < astacktraceelement.length) {
                this.uncategorizedStackTrace = new StackTraceElement[k];
                System.arraycopy(astacktraceelement, 0, this.uncategorizedStackTrace, 0, this.uncategorizedStackTrace.length);
            } else {
                this.trackingStackTrace = false;
            }
        }

        this.details.add(crashreportsystemdetails);
        return crashreportsystemdetails;
    }

    private static String getErrorComment() {
        String[] astring = new String[]{"누가 TNT를 설치했어?", "모든것이 계획대로 가고있어, 어... 그건 원래 계획에 있었어", "어.... 내가 했나?", "이런.", "내가 왜 이런짓을 했지?", "슬퍼 :(", "내 잘못이야.", "미안해 데이브", "널 슬프게 했어, 미안해 :(", "다른면은, 내가 곰돌이를 가져왔어!", "데이지... 데이지...", "아! 뭘 잘못했는지 알겠어!", "간지러워, 히히", "Dinnerbone 탓이야", "우리 누나가만든 게임 Minecraft를 플레이해봐!", "슬프지마, 내가 정말로 나중에 더 잘할께!", "슬프지마, 내가 안아줄께", "뭐가 잘못됐는지 모르겠어...", "게임을 플레이할래?", "솔직히 말해사, 난 그걸 걱정하지 않을꺼야.", "Cylons는 이 문제가 없다는거에 걸지", "미안해 :(", "서프라이즈! 어... 이건 어색하네", "컵케이크를 원하니?", "안녕, 난 Minecraft이고 난 crashaholic이야.", "오... 반짝거리는데...", "이건 아무런 연관이 되지 않아!", "왜 고장나는거지 :(", "하지마.", "아야, 아프네", "너는 나빴어.", "이건 한번 무료로 안을수 있는 토큰이야, 가까운 Mojansta에서 받으세요: [~~HUG~~]", "4개의 빛이 있어!", "하지만, 내 컴퓨터에서는 작동했는데?"};

        try {
            return astring[(int) (SystemUtils.getNanos() % (long) astring.length)];
        } catch (Throwable throwable) {
            return "재치있는 문장을 적을려다가 생각나는게 없어요 :(";
        }
    }

    public static CrashReport forThrowable(Throwable throwable, String s) {
        while (throwable instanceof CompletionException && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        CrashReport crashreport;

        if (throwable instanceof ReportedException) {
            crashreport = ((ReportedException) throwable).getReport();
        } else {
            crashreport = new CrashReport(s, throwable);
        }

        return crashreport;
    }

    public static void preload() {
        MemoryReserve.allocate();
        (new CrashReport("패닉하지 마세요!", new Throwable())).getFriendlyReport();
    }
}
