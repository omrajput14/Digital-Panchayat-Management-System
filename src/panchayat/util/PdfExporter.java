package panchayat.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PdfExporter — Pure-Java PDF 1.4 generator. No external libraries.
 *
 * Generates A4 documents using built-in Type1 fonts (Helvetica, Courier).
 * Absolute text positioning via the Tm operator avoids cumulative drift.
 * The xref table is built in a second pass once all byte offsets are known.
 */
public final class PdfExporter {

    private PdfExporter() {}

    // ── A4 page geometry (points: 1pt = 1/72 inch) ──────────────────────
    private static final int W = 595, H = 842;
    private static final int ML = 50, MR = 50;
    private static final int HEADER_H = 70, FOOTER_H = 40;
    private static final int BODY_TOP = H - HEADER_H - 8;
    private static final int BODY_BOT = FOOTER_H + 8;

    // ── Styled line representation ───────────────────────────────────────
    private enum Style { NORMAL, BOLD, MONO, SEPARATOR }

    private record Line(String text, int size, Style style) {
        static Line sep()  { return new Line("", 4,  Style.SEPARATOR); }
        static Line body(String t)   { return new Line(t, 10, Style.NORMAL); }
        static Line bold(String t)   { return new Line(t, 12, Style.BOLD); }
        static Line mono(String t)   { return new Line(t, 10, Style.MONO); }
    }

    // ════════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════════

    /**
     * Opens a JFileChooser and writes the report to the chosen PDF file.
     * No print dialog is shown — the file is saved directly to disk.
     */
    public static void savePdf(String title, String body, java.awt.Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save PDF Report");
        fc.setSelectedFile(new File(title.replaceAll("[^A-Za-z0-9]", "_") + ".pdf"));
        fc.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));

        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf"))
            f = new File(f.getAbsolutePath() + ".pdf");

        try {
            byte[] pdf = buildPdf(title, body);
            try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(pdf); }
            JOptionPane.showMessageDialog(parent,
                "\u2705  PDF saved to:\n" + f.getAbsolutePath(),
                "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                "Error saving PDF:\n" + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PDF assembly
    // ════════════════════════════════════════════════════════════════════

    static byte[] buildPdf(String title, String body) throws IOException {
        String ts = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

        List<Line> lines = parseBody(body);
        List<List<Integer>> pages = paginate(lines); // list of line-index lists per page
        int P = pages.size();

        // Object number plan:
        //  1        = Catalog
        //  2        = Pages
        //  3..3+P-1 = Page objects
        //  3+P..3+2P-1 = Content streams
        //  3+2P     = Font F1 (Helvetica)
        //  3+2P+1   = Font F2 (Helvetica-Bold)
        //  3+2P+2   = Font F3 (Courier)
        //  total    = 3+2P+2

        int catN  = 1, pagesN = 2;
        int pg0   = 3,     cs0 = 3 + P;
        int fHelv = 3+2*P, fBold = 4+2*P, fMono = 5+2*P;
        int total = 5 + 2*P;

        ByteArrayOutputStream out  = new ByteArrayOutputStream(32768);
        long[] off = new long[total + 1]; // off[n] = byte offset of object n

        // PDF header
        w(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

        // 1: Catalog
        off[catN] = out.size();
        w(out, obj(catN, "<< /Type /Catalog /Pages " + pagesN + " 0 R >>"));

        // 2: Pages
        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < P; i++) kids.append(pg0+i).append(" 0 R ");
        kids.append("]");
        off[pagesN] = out.size();
        w(out, obj(pagesN, "<< /Type /Pages /Kids "+kids+" /Count "+P+" >>"));

        // 3..3+P-1: Page objects
        String fonts = "<< /F1 "+fHelv+" 0 R /F2 "+fBold+" 0 R /F3 "+fMono+" 0 R >>";
        for (int i = 0; i < P; i++) {
            off[pg0+i] = out.size();
            w(out, obj(pg0+i,
                "<< /Type /Page /Parent "+pagesN+" 0 R\n"
                +"   /MediaBox [0 0 "+W+" "+H+"]\n"
                +"   /Contents "+(cs0+i)+" 0 R\n"
                +"   /Resources << /Font "+fonts+" >> >>"));
        }

        // 3+P..3+2P-1: Content streams
        for (int i = 0; i < P; i++) {
            byte[] cs = buildContent(lines, pages.get(i), i+1, P, title, ts);
            off[cs0+i] = out.size();
            w(out, (cs0+i)+" 0 obj\n<< /Length "+cs.length+" >>\nstream\n");
            out.write(cs);
            w(out, "\nendstream\nendobj\n");
        }

        // Font objects
        off[fHelv] = out.size();
        w(out, obj(fHelv, font("Helvetica")));
        off[fBold] = out.size();
        w(out, obj(fBold, font("Helvetica-Bold")));
        off[fMono] = out.size();
        w(out, obj(fMono, font("Courier")));

        // xref table
        long xrefPos = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 ").append(total+1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= total; i++)
            xref.append(String.format("%010d 00000 n \n", off[i]));
        w(out, xref.toString());

        // Trailer
        w(out, "trailer\n<< /Size "+(total+1)+" /Root "+catN+" 0 R >>\n"
             + "startxref\n"+xrefPos+"\n%%EOF\n");

        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════════
    // Content stream builder (one page)
    // ════════════════════════════════════════════════════════════════════

    private static byte[] buildContent(List<Line> lines, List<Integer> indices,
                                        int pageNum, int total,
                                        String title, String ts) {
        StringBuilder s = new StringBuilder(2048);

        // ── Header background ─────────────────────────────────────────
        s.append("q\n0.08 0.09 0.16 rg\n")
         .append("0 ").append(H-HEADER_H).append(' ').append(W).append(' ').append(HEADER_H).append(" re f\nQ\n");

        // ── Header text ───────────────────────────────────────────────
        s.append("BT\n1 1 1 rg\n/F2 14 Tf\n");
        tm(s, ML, H-30); s.append('(').append(esc("Digital Panchayat Management System")).append(") Tj\n");
        s.append("/F1 10 Tf\n");
        tm(s, ML, H-48); s.append('(').append(esc(title)).append(") Tj\n");
        s.append("0.58 0.64 0.72 rg\n/F1 8 Tf\n");
        tm(s, ML, H-61); s.append('(').append(esc("Generated: " + ts)).append(") Tj\n");
        s.append("ET\n");

        // ── Body lines ────────────────────────────────────────────────
        s.append("BT\n");
        int y = BODY_TOP;

        for (int idx : indices) {
            Line line = lines.get(idx);
            int lh = line.size() + 5;

            if (line.style() == Style.SEPARATOR) {
                s.append("ET\n");
                s.append("q\n0.7 0.7 0.7 RG\n0.5 w\n")
                 .append(ML).append(' ').append(y).append(" m ")
                 .append(W-MR).append(' ').append(y).append(" l S\nQ\n");
                y -= 8;
                s.append("BT\n");
                continue;
            }

            String fnt = switch (line.style()) {
                case BOLD -> "/F2";
                case MONO -> "/F3";
                default   -> "/F1";
            };
            float[] col = line.style() == Style.BOLD
                ? new float[]{0.23f, 0.51f, 0.96f}
                : new float[]{0.1f,  0.1f,  0.1f};

            s.append(fnt).append(' ').append(line.size()).append(" Tf\n");
            s.append(col[0]).append(' ').append(col[1]).append(' ').append(col[2]).append(" rg\n");
            tm(s, ML, y);
            s.append('(').append(esc(line.text())).append(") Tj\n");
            y -= lh;
        }
        s.append("ET\n");

        // ── Footer ───────────────────────────────────────────────────
        s.append("q\n0.8 0.8 0.8 RG\n0.5 w\n")
         .append(ML).append(" 42 m ").append(W-MR).append(" 42 l S\nQ\n");
        s.append("BT\n0.5 0.5 0.5 rg\n/F1 8 Tf\n");
        tm(s, ML, 30);
        s.append("(Gram Panchayat Office \u2014 Confidential) Tj\n");
        tm(s, W-MR-60, 30);
        s.append("(Page ").append(pageNum).append(" of ").append(total).append(") Tj\n");
        s.append("ET\n");

        return s.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    // ════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════

    /** Parse the body text into a list of styled lines. */
    private static List<Line> parseBody(String body) {
        List<Line> res = new ArrayList<>();
        for (String raw : body.split("\n")) {
            if (raw.startsWith("\u2550") || raw.startsWith("\u2500") || raw.matches("[=-]{5,}.*")) {
                res.add(Line.sep());
            } else if (raw.startsWith("SECTION:")) {
                res.add(Line.bold(raw.replace("SECTION:", "").trim()));
            } else if (raw.trim().startsWith("\u25b6") || raw.trim().startsWith(">")) {
                res.addAll(wrapLine("  " + raw.trim(), Style.MONO, 10));
            } else {
                res.addAll(wrapLine(raw, Style.NORMAL, 10));
            }
        }
        return res;
    }

    /** Wrap a single text string into multiple lines fitting the usable width. */
    private static List<Line> wrapLine(String text, Style style, int size) {
        if (text.isBlank()) return List.of(new Line("", size, style));
        double cw   = (style == Style.MONO) ? size * 0.6 : size * 0.5;
        int    limit = (int)((W - ML - MR) / cw);
        List<Line> res = new ArrayList<>();
        while (text.length() > limit) {
            int cut = text.lastIndexOf(' ', limit);
            if (cut <= 0) cut = limit;
            res.add(new Line(text.substring(0, cut), size, style));
            text = "  " + text.substring(cut).trim();
        }
        res.add(new Line(text, size, style));
        return res;
    }

    /** Distribute lines across pages based on available vertical space. */
    private static List<List<Integer>> paginate(List<Line> lines) {
        List<List<Integer>> pages = new ArrayList<>();
        List<Integer> cur = new ArrayList<>();
        int y = BODY_TOP;

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            int lh = (l.style() == Style.SEPARATOR) ? 8 : l.size() + 5;
            if (y - lh < BODY_BOT && !cur.isEmpty()) {
                pages.add(cur); cur = new ArrayList<>(); y = BODY_TOP;
            }
            cur.add(i);
            y -= lh;
        }
        if (!cur.isEmpty()) pages.add(cur);
        if (pages.isEmpty()) pages.add(new ArrayList<>());
        return pages;
    }

    /** PDF object wrapper. */
    private static String obj(int n, String dict) {
        return n + " 0 obj\n" + dict + "\nendobj\n";
    }

    /** Font dictionary string for a built-in Type1 font. */
    private static String font(String name) {
        return "<< /Type /Font /Subtype /Type1 /BaseFont /" + name
             + " /Encoding /WinAnsiEncoding >>";
    }

    /** Append an absolute text-matrix (Tm) operator. */
    private static void tm(StringBuilder s, int x, int y) {
        s.append("1 0 0 1 ").append(x).append(' ').append(y).append(" Tm\n");
    }

    /** Write ISO-8859-1 bytes to output stream. */
    private static void w(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Escape PDF string special characters and replace non-Latin-1 glyphs
     * with ASCII equivalents so Helvetica/Courier can render them.
     */
    static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                case '\\' -> sb.append("\\\\");
                // Common unicode → ASCII fallbacks
                case '\u2550', '\u2500', '\u2014', '\u2013' -> sb.append('-');
                case '\u25b6', '\u00bb', '\u2192' -> sb.append('>');
                case '\u2026' -> sb.append("...");
                case '\u2019', '\u2018' -> sb.append('\'');
                case '\u201c', '\u201d' -> sb.append('"');
                case '\u2714', '\u2713' -> sb.append("[OK]");
                case '\u274c' -> sb.append("[X]");
                case '\t' -> sb.append("    ");
                default -> {
                    if (c < 256) sb.append(c);
                    else sb.append('?');
                }
            }
        }
        return sb.toString();
    }
}
