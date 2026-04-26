package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "course")
public class Course {

  @Id
  @Column(name = "cno", nullable = false, length = 8)
  private String cno;

  @Column(name = "cname", nullable = false, length = 20)
  private String cname;

  @Column(name = "credit", nullable = false, precision = 2, scale = 1)
  private BigDecimal credit;

  public String getCno() {
    return cno;
  }

  public void setCno(String cno) {
    this.cno = cno;
  }

  public String getCname() {
    return cname;
  }

  public void setCname(String cname) {
    this.cname = cname;
  }

  public BigDecimal getCredit() {
    return credit;
  }

  public void setCredit(BigDecimal credit) {
    this.credit = credit;
  }
}