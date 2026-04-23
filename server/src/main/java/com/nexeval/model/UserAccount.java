package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  @Column(name = "id", nullable = false, length = 9)
  private String id;

  @Column(name = "name", nullable = false, length = 20)
  private String name;

  @Column(name = "sex", nullable = false)
  private boolean sex;

  @Column(name = "type", nullable = false, length = 16)
  private String type;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Column(name = "phone", nullable = false, length = 11)
  private String phone;

  @Column(name = "email", length = 254)
  private String email;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isSex() {
    return sex;
  }

  public void setSex(boolean sex) {
    this.sex = sex;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
