package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "paper_publish")
public class PaperPublish {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "definition_id", nullable = false, length = 32)
  private String definitionId;

  @Column(name = "cno", nullable = false, length = 8)
  private String cno;

  @Column(name = "eid", nullable = false, length = 8)
  private String eid;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getDefinitionId() { return definitionId; }
  public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
  public String getCno() { return cno; }
  public void setCno(String cno) { this.cno = cno; }
  public String getEid() { return eid; }
  public void setEid(String eid) { this.eid = eid; }
}
