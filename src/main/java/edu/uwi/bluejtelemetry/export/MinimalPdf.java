package edu.uwi.bluejtelemetry.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Self-contained PDF 1.4 writer. No PDF-related runtime dependencies. */
final class MinimalPdf {
    private final List<Page> pages = new ArrayList<>();
    Page addPage() { Page p = new Page(); pages.add(p); return p; }
    static final class Page {
        private final StringBuilder commands = new StringBuilder();
        void fill(double r, double g, double b) { commands.append(fmt(r)+" "+fmt(g)+" "+fmt(b)+" rg\n"); }
        void stroke(double r, double g, double b) { commands.append(fmt(r)+" "+fmt(g)+" "+fmt(b)+" RG\n"); }
        void rectangle(double x, double y, double w, double h) {
            commands.append(fmt(x)+" "+fmt(y)+" "+fmt(w)+" "+fmt(h)+" re f\n");
        }
        void line(double x, double y, double xx, double yy, double width) {
            commands.append(fmt(width)+" w "+fmt(x)+" "+fmt(y)+" m "+fmt(xx)+" "+fmt(yy)+" l S\n");
        }
        void text(String value, double x, double y, double size, boolean bold) {
            String safe = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
            safe = safe.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            commands.append("BT /"+(bold?"FB":"FR")+" "+fmt(size)+" Tf "+fmt(x)+" "+fmt(y)+" Td ("+safe+") Tj ET\n");
        }
        String content() { return commands.toString(); }
    }
    private static String fmt(double d) { return String.format(Locale.ROOT, "%.2f", d); }
    byte[] toBytes() throws IOException {
        if (pages.isEmpty()) throw new IllegalStateException("PDF needs at least one page");
        int n = pages.size();
        int pageStart = 5;
        StringBuilder kids = new StringBuilder();
        for (int i=0; i<n; i++) kids.append(pageStart+2*i).append(" 0 R ");
        List<String> objs = new ArrayList<>();
        objs.add("<< /Type /Catalog /Pages 2 0 R >>");
        objs.add("<< /Type /Pages /Kids ["+kids+"] /Count "+n+" >>");
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        for (int i=0; i<n; i++) {
            int stream = pageStart + 2*i + 1;
            objs.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Resources << /Font << /FR 3 0 R /FB 4 0 R >> >> /Contents "+stream+" 0 R >>");
            String commands = pages.get(i).content();
            objs.add("<< /Length "+commands.getBytes(StandardCharsets.ISO_8859_1).length+" >>\nstream\n"+commands+"endstream");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i=0; i<objs.size(); i++) {
            offsets.add(out.size());
            out.write((i+1+" 0 obj\n"+objs.get(i)+"\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 "+(objs.size()+1)+"\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i=1; i<offsets.size(); i++)
            out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        out.write(("trailer\n<< /Size "+(objs.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n")
            .getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }
    void save(Path output) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, toBytes());
    }
}
