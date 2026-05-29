package com.nexeval.controller;

import com.nexeval.dto.QuestionCreateRequest;
import com.nexeval.service.QuestionBankManagementService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question-bank")
public class QuestionBankController {

  private final QuestionBankManagementService questionBankManagementService;

  public QuestionBankController(QuestionBankManagementService questionBankManagementService) {
    this.questionBankManagementService = questionBankManagementService;
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createQuestion(@RequestBody QuestionCreateRequest request) {
    return ResponseEntity.ok(questionBankManagementService.createQuestion(request));
  }
}
