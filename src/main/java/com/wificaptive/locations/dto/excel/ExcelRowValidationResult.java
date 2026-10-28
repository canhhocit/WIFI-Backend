package com.wificaptive.locations.dto.excel;

import java.util.ArrayList;
import java.util.List;

public class ExcelRowValidationResult {
    private int rowNumber;
    private String code;
    private String name;
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();
    private LocationImportRowData data;

    public ExcelRowValidationResult() {}

    public ExcelRowValidationResult(int rowNumber, String code, String name) {
        this.rowNumber = rowNumber;
        this.code = code;
        this.name = name;
    }

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public LocationImportRowData getData() { return data; }
    public void setData(LocationImportRowData data) { this.data = data; }
}

