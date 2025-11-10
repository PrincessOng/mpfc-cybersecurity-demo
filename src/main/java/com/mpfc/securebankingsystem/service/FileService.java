package com.mpfc.securebankingsystem.service;

import com.mpfc.securebankingsystem.entity.FileEncrypted;
import com.mpfc.securebankingsystem.repo.EncryptedFileRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;

// Add POI imports:
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class FileService {

    private final EncryptionService encryptionService;
    private final EncryptedFileRepository repo;
    private final AuditLogService audit;

    public FileService(EncryptionService encryptionService, EncryptedFileRepository repo, AuditLogService audit) {
        this.encryptionService = encryptionService;
        this.repo = repo;
        this.audit = audit;
    }

    public FileEncrypted uploadAndEncrypt(String username, MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");

        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (!(contentType.equals("text/csv") ||
              contentType.equals("application/vnd.ms-excel") ||
              contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
              contentType.equals("application/octet-stream"))) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        byte[] bytes = file.getBytes();

        // Content validation by type
        if (isCsv(file)) {
            validateMpfcMemberCsv(bytes);
        } else if (isXlsx(file) || isXls(file)) {
            validateMpfcMemberExcel(bytes, isXlsx(file));
        }

        String checksum = sha256Hex(bytes);
        if (repo.findByChecksumSha256(checksum).isPresent()) {
            throw new IllegalArgumentException("Duplicate file (same checksum) detected");
        }

        var result = encryptionService.encrypt(bytes);

        FileEncrypted ef = new FileEncrypted();
        ef.setFileName(file.getOriginalFilename());
        ef.setContentType(contentType);
        ef.setSizeBytes(file.getSize());
        ef.setUploader(username);
        ef.setUploadedAt(Instant.now());
        ef.setEncryptionAlgo("AES/GCM/NoPadding");
        ef.setIv(result.iv());
        ef.setCipherData(result.cipher());
        ef.setChecksumSha256(checksum);

        FileEncrypted saved = repo.save(ef);
        audit.log(username, "FILE_ENCRYPTED", String.valueOf(saved.getId()), "Uploaded and encrypted file");
        return saved;
    }

    public byte[] decryptForAdmin(String username, Long id) throws Exception {
        var ef = repo.findById(id).orElseThrow();
        byte[] plain = encryptionService.decrypt(ef.getIv(), ef.getCipherData());
        audit.log(username, "FILE_DECRYPTED", String.valueOf(id), "Decrypted file for viewing");
        return plain;
    }

    private boolean isCsv(MultipartFile file) {
        String ct = file.getContentType() != null ? file.getContentType() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        return ct.equals("text/csv") || name.endsWith(".csv");
    }
    private boolean isXlsx(MultipartFile file) {
        String ct = file.getContentType() != null ? file.getContentType() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        return ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || name.endsWith(".xlsx");
    }
    private boolean isXls(MultipartFile file) {
        String ct = file.getContentType() != null ? file.getContentType() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        return ct.equals("application/vnd.ms-excel") || name.endsWith(".xls");
    }

    // CSV validation (exact ordered headers + row checks)
    private void validateMpfcMemberCsv(byte[] bytes) {
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
        String[] lines = content.split("\\R");
        if (lines.length < 2) {
            throw new IllegalArgumentException("CSV must contain a header and at least one data row");
        }

        String[] expectedOrdered = {"MemberID","FullName","Address","AccountNumber","Balance","LastTransactionDate"};
        java.util.List<String> header = parseCsvLine(lines[0]).stream().map(String::trim).toList();

        if (header.size() != expectedOrdered.length) {
            throw new IllegalArgumentException("Header count mismatch");
        }
        for (int i = 0; i < expectedOrdered.length; i++) {
            if (!expectedOrdered[i].equals(header.get(i))) {
                throw new IllegalArgumentException("Header mismatch at position " + (i+1) +
                        ": expected '" + expectedOrdered[i] + "' got '" + header.get(i) + "'");
            }
        }

        java.util.Map<String,Integer> idx = new java.util.HashMap<>();
        for (int i=0;i<header.size();i++) idx.put(header.get(i), i);

        java.util.Set<String> memberIds = new java.util.HashSet<>();
        for (int r=1; r<lines.length; r++) {
            String line = lines[r].trim();
            if (line.isEmpty()) continue;

            java.util.List<String> cols = parseCsvLine(line);
            if (cols.size() != header.size()) {
                throw new IllegalArgumentException("Row " + r + " column count mismatch");
            }

            validateRowFields(r, cols.get(idx.get("MemberID")), cols.get(idx.get("FullName")),
                    cols.get(idx.get("AccountNumber")), cols.get(idx.get("Balance")), cols.get(idx.get("LastTransactionDate")), memberIds);
        }
    }

    // Excel validation (XLSX/XLS)
    private void validateMpfcMemberExcel(byte[] bytes, boolean xlsx) throws IllegalArgumentException {
        String[] expectedOrdered = {"MemberID","FullName","Address","AccountNumber","Balance","LastTransactionDate"};

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Workbook wb = xlsx ? new XSSFWorkbook(bais) : new HSSFWorkbook(bais)) {

            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) throw new IllegalArgumentException("Workbook has no sheets");

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Missing header row");

            DataFormatter fmt = new DataFormatter();
            if (headerRow.getLastCellNum() != expectedOrdered.length) {
                throw new IllegalArgumentException("Header count mismatch");
            }
            for (int i=0; i<expectedOrdered.length; i++) {
                String val = fmt.formatCellValue(headerRow.getCell(i)).trim();
                if (!expectedOrdered[i].equals(val)) {
                    throw new IllegalArgumentException("Header mismatch at position " + (i+1) +
                        ": expected '" + expectedOrdered[i] + "' got '" + val + "'");
                }
            }

            java.util.Set<String> memberIds = new java.util.HashSet<>();
            int lastRow = sheet.getLastRowNum();
            for (int r=1; r<=lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                // Ensure column count
                if (row.getLastCellNum() != expectedOrdered.length) {
                    throw new IllegalArgumentException("Row " + r + " column count mismatch");
                }
                String memberId = fmt.formatCellValue(row.getCell(0)).trim();
                String fullName = fmt.formatCellValue(row.getCell(1)).trim();
                String account  = fmt.formatCellValue(row.getCell(3)).trim();
                String balance  = fmt.formatCellValue(row.getCell(4)).trim();
                String date     = fmt.formatCellValue(row.getCell(5)).trim();

                validateRowFields(r, memberId, fullName, account, balance, date, memberIds);
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Excel format: " + ex.getMessage());
        }
    }

    private void validateRowFields(int r, String memberId, String fullName, String account, String balanceStr, String dateStr, java.util.Set<String> memberIds) {
        if (memberId.isEmpty()) throw new IllegalArgumentException("Row " + r + ": MemberID required");
        if (!memberIds.add(memberId)) throw new IllegalArgumentException("Duplicate MemberID: " + memberId);
        if (fullName.isEmpty()) throw new IllegalArgumentException("Row " + r + ": FullName required");
        if (!account.matches("\\d{8,16}")) throw new IllegalArgumentException("Row " + r + ": AccountNumber 8-16 digits");
        try {
            double bal = Double.parseDouble(balanceStr);
            if (bal < 0) throw new IllegalArgumentException("Row " + r + ": Balance negative");
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Row " + r + ": Balance numeric");
        }
        try {
            java.time.LocalDate.parse(dateStr); // yyyy-MM-dd
        } catch (java.time.format.DateTimeParseException dte) {
            throw new IllegalArgumentException("Row " + r + ": Date must be yyyy-MM-dd");
        }
    }

    // Minimal CSV parser supporting quotes and commas inside quotes
    private java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0; i<line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"'); // escaped quote
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(data);
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
