package com.nexeval.dto;

import java.util.List;

public record BulkImportResult(
  String importType,
  int imported,
  int skipped,
  int failed,
  List<String> errors
) {
}