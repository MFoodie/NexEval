package com.nexeval.controller;

import com.nexeval.dto.WeaknessExplainRequest;
import com.nexeval.dto.WeaknessExplainResponse;
import com.nexeval.dto.WeaknessQuestionRequest;
import com.nexeval.dto.WeaknessQuestionResponse;
import com.nexeval.service.AiWeaknessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/weakness")
public class AiWeaknessController {

  private final AiWeaknessService aiWeaknessService;

  public AiWeaknessController(AiWeaknessService aiWeaknessService) {
    this.aiWeaknessService = aiWeaknessService;
  }

  @PostMapping("/question")
  public ResponseEntity<WeaknessQuestionResponse> generateQuestion(
    @RequestBody WeaknessQuestionRequest request
  ) {
    return ResponseEntity.ok(aiWeaknessService.generateQuestion(request));
  }

  @PostMapping("/explain")
  public ResponseEntity<WeaknessExplainResponse> generateExplanation(
    @RequestBody WeaknessExplainRequest request
  ) {
    return ResponseEntity.ok(aiWeaknessService.generateExplanation(request));
  }
}
