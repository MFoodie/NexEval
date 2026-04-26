package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ScRecordId implements Serializable {

  @Column(name = "sno", nullable = false, length = 8)
  private String sno;

  @Column(name = "cno", nullable = false, length = 8)
  private String cno;

  @Column(name = "eid", nullable = false, length = 8)
  private String eid;

  public String getSno() {
    return sno;
  }

  public void setSno(String sno) {
    this.sno = sno;
  }

  public String getCno() {
    return cno;
  }

  public void setCno(String cno) {
    this.cno = cno;
  }

  public String getEid() {
    return eid;
  }

  public void setEid(String eid) {
    this.eid = eid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScRecordId that = (ScRecordId) o;
    return Objects.equals(sno, that.sno)
      && Objects.equals(cno, that.cno)
      && Objects.equals(eid, that.eid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sno, cno, eid);
  }
}