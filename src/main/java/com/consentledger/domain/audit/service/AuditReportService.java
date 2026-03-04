package com.consentledger.domain.audit.service;

import com.consentledger.domain.audit.entity.AuditLog;
import com.consentledger.domain.audit.repository.AuditLogRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditReportService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    private final AuditLogRepository auditLogRepository;
    private final HashChainService hashChainService;

    @Transactional(readOnly = true)
    public byte[] generatePdfReport(Instant from, Instant to) {
        List<AuditLog> logs = auditLogRepository.findByTsBetweenOrderByTsAsc(from, to);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            addTitle(document, from, to);
            addSummary(document, logs);
            addLogTable(document, logs);

            document.close();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addTitle(Document doc, Instant from, Instant to) {
        doc.add(new Paragraph("ConsentLedger Audit Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(String.format("Period: %s ~ %s",
                FORMATTER.format(from), FORMATTER.format(to)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(String.format("Generated: %s", FORMATTER.format(Instant.now())))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("\n"));
    }

    private void addSummary(Document doc, List<AuditLog> logs) {
        doc.add(new Paragraph("Summary").setFontSize(14).setBold());

        Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));

        doc.add(new Paragraph("Total Events: " + logs.size()).setFontSize(11));

        actionCounts.forEach((action, count) ->
                doc.add(new Paragraph(String.format("  - %s: %d", action, count)).setFontSize(10)));

        doc.add(new Paragraph("\n"));
    }

    private void addLogTable(Document doc, List<AuditLog> logs) {
        doc.add(new Paragraph("Audit Log Entries").setFontSize(14).setBold());

        float[] columnWidths = {1, 2, 2, 2, 2, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

        addHeaderCell(table, "ID");
        addHeaderCell(table, "Timestamp");
        addHeaderCell(table, "Action");
        addHeaderCell(table, "Object");
        addHeaderCell(table, "Actor");
        addHeaderCell(table, "Outcome");

        for (AuditLog logEntry : logs) {
            table.addCell(String.valueOf(logEntry.getId()));
            table.addCell(FORMATTER.format(logEntry.getTs()));
            table.addCell(logEntry.getAction());
            table.addCell(logEntry.getObjectType() + "\n" +
                    (logEntry.getObjectId() != null ? logEntry.getObjectId().toString().substring(0, 8) + "..." : "-"));
            table.addCell(logEntry.getActorUserId() != null
                    ? "User:" + logEntry.getActorUserId().toString().substring(0, 8) + "..."
                    : logEntry.getActorAgentId() != null
                    ? "Agent:" + logEntry.getActorAgentId().toString().substring(0, 8) + "..."
                    : "-");
            table.addCell(logEntry.getOutcome());
        }

        doc.add(table);
    }

    private void addHeaderCell(Table table, String text) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
    }
}
