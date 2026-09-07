package w4cash.print;

import java.time.LocalDateTime;

public class PrintJob {

    private Long id;
    private String tableId;
    private String tableName;
    private int printerIndex;
    private String printerName;
    private int lineCount;
    private LocalDateTime printedAt;
    private boolean success;
    private String error;
    private String content;
    private String personId;
    private String personName;

    public PrintJob() {}

    public PrintJob(String tableId, String tableName, int printerIndex, String printerName,
                    int lineCount, LocalDateTime printedAt, boolean success, String error, String content,
                    String personId, String personName) {
        this.tableId = tableId;
        this.tableName = tableName;
        this.printerIndex = printerIndex;
        this.printerName = printerName;
        this.lineCount = lineCount;
        this.printedAt = printedAt;
        this.success = success;
        this.error = error;
        this.content = content;
        this.personId = personId;
        this.personName = personName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public int getPrinterIndex() { return printerIndex; }
    public void setPrinterIndex(int printerIndex) { this.printerIndex = printerIndex; }

    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
}
