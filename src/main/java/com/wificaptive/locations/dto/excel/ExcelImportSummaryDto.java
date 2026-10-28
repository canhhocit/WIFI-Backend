package com.wificaptive.locations.dto.excel;

import java.util.ArrayList;
import java.util.List;

public class ExcelImportSummaryDto {
    private String importSessionId;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private List<ExcelRowValidationResult> rowResults = new ArrayList<>();

    public ExcelImportSummaryDto() {}

    public String getImportSessionId() { return importSessionId; }
    public void setImportSessionId(String importSessionId) { this.importSessionId = importSessionId; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getErrorRows() { return errorRows; }
    public void setErrorRows(int errorRows) { this.errorRows = errorRows; }

    public List<ExcelRowValidationResult> getRowResults() { return rowResults; }
    public void setRowResults(List<ExcelRowValidationResult> rowResults) { this.rowResults = rowResults; }
}

