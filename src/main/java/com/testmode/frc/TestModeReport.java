package com.testmode.frc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import edu.wpi.first.net.WebServer;

/**
 * Generates an HTML report summarizing test mode results.
 *
 * <p>The report is written to {@code /home/lvuser/test-report.html} and,
 * if a USB drive is mounted at {@code /U/}, also to {@code /U/test-report.html}.
 *
 * <p>Implements {@link Consumer}{@code <List<TestResult>>} so it can be added to a
 * {@link TestModeBuilder} via {@code withTestResultConsumer(new TestModeReport())}.
 */
public class TestModeReport implements Consumer<List<TestResult>> {
    private static final String PRIMARY_PATH = "/home/lvuser/test-report.html";
    private static final String USB_PATH = "/U/test-report.html";

    @Override
    public void accept(List<TestResult> results) {
        serveReport(results);
    }

    /**
     * Generates and writes the HTML report for the given results.
     *
     * @param results list of results from each completed test step
     */
    public static void serveReport(List<TestResult> results) {
        String html = buildHtml(results);
        writeTo(PRIMARY_PATH, html);
        writeTo(USB_PATH, html);
        WebServer.start(5800, html);
    }

    public static void stopWebServer() {
        WebServer.stop(5800);
    }

    private static void writeTo(String path, String html) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                return;
            }
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.print(html);
            }
        } catch (IOException e) {
            System.err.println("[TestModeReport] Could not write report to " + path + ": " + e.getMessage());
        }
    }

    private static String buildHtml(List<TestResult> results) {
        long passCount = results.stream().filter(r -> r.passed).count();
        long failCount = results.size() - passCount;
        String overallStatus = failCount == 0 ? "PASS" : "FAIL";
        String overallColor = failCount == 0 ? "#2e7d32" : "#c62828";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Test Mode Report</title>\n");
        sb.append("<style>\n");
        sb.append("  body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 24px; background: #f5f5f5; color: #212121; }\n");
        sb.append("  h1 { margin: 0 0 4px 0; font-size: 1.8em; }\n");
        sb.append("  .meta { color: #666; font-size: 0.9em; margin-bottom: 24px; }\n");
        sb.append("  .summary { display: flex; gap: 16px; margin-bottom: 28px; }\n");
        sb.append("  .badge { padding: 12px 24px; border-radius: 8px; font-weight: bold; font-size: 1.1em; color: #fff; }\n");
        sb.append("  .badge-overall { background: " + overallColor + "; font-size: 1.4em; }\n");
        sb.append("  .badge-pass { background: #2e7d32; }\n");
        sb.append("  .badge-fail { background: #c62828; }\n");
        sb.append("  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.12); }\n");
        sb.append("  th { background: #1565c0; color: #fff; padding: 12px 16px; text-align: left; font-size: 0.95em; }\n");
        sb.append("  td { padding: 11px 16px; border-bottom: 1px solid #e0e0e0; font-size: 0.95em; }\n");
        sb.append("  tr:last-child td { border-bottom: none; }\n");
        sb.append("  tr:nth-child(even) { background: #f9f9f9; }\n");
        sb.append("  .status-pass { color: #2e7d32; font-weight: bold; }\n");
        sb.append("  .status-fail { color: #c62828; font-weight: bold; }\n");
        sb.append("  .bar-wrap { background: #e0e0e0; border-radius: 4px; height: 10px; min-width: 80px; overflow: hidden; }\n");
        sb.append("  .bar-fill { height: 10px; border-radius: 4px; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<h1>Test Mode Report</h1>\n");
        sb.append("<div class=\"meta\">Generated: ").append(timestamp).append("</div>\n");

        sb.append("<div class=\"summary\">\n");
        sb.append("  <div class=\"badge badge-overall\">").append(overallStatus).append("</div>\n");
        if (passCount > 0) {
            sb.append("  <div class=\"badge badge-pass\">").append(passCount).append(" PASS</div>\n");
        }
        if (failCount > 0) {
            sb.append("  <div class=\"badge badge-fail\">").append(failCount).append(" FAIL</div>\n");
        }
        sb.append("</div>\n");

        sb.append("<table>\n<thead>\n<tr>\n");
        sb.append("  <th>Step</th>\n");
        sb.append("  <th>Status</th>\n");
        sb.append("  <th>Measured Velocity</th>\n");
        sb.append("  <th>Baseline Velocity</th>\n");
        sb.append("  <th>Deviation</th>\n");
        sb.append("  <th>Ratio</th>\n");
        sb.append("</tr>\n</thead>\n<tbody>\n");

        for (TestResult r : results) {
            double deviation = r.baselineVelocity != 0
                ? (r.averageVelocity - r.baselineVelocity) / r.baselineVelocity * 100.0
                : 0.0;
            double ratio = r.baselineVelocity != 0
                ? r.averageVelocity / r.baselineVelocity
                : 1.0;
            int barPct = (int) Math.min(100, Math.max(0, ratio * 100));
            String barColor = r.passed ? "#2e7d32" : "#c62828";

            sb.append("<tr>\n");
            sb.append("  <td>").append(escape(r.stepName)).append("</td>\n");
            sb.append("  <td class=\"").append(r.passed ? "status-pass" : "status-fail").append("\">")
              .append(r.passed ? "PASS" : "FAIL").append("</td>\n");
            sb.append("  <td>").append(String.format("%.2f", r.averageVelocity)).append("</td>\n");
            sb.append("  <td>").append(String.format("%.2f", r.baselineVelocity)).append("</td>\n");
            sb.append("  <td>").append(String.format("%+.1f%%", deviation)).append("</td>\n");
            sb.append("  <td><div class=\"bar-wrap\"><div class=\"bar-fill\" style=\"width:")
              .append(barPct).append("%;background:").append(barColor).append("\"></div></div></td>\n");
            sb.append("</tr>\n");
        }

        sb.append("</tbody>\n</table>\n</body>\n</html>\n");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
