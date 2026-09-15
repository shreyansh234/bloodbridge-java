package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="accounts")
public class Account {
 @Id @Column(length=36) public String id = UUID.randomUUID().toString();
 @Column(nullable=false,length=80) public String name;
 @Column(length=180) public String email;
 @Column(name="password_hash",length=100) public String passwordHash;
 @Column(name="external_id",unique=true,length=80) public String externalId;
 @Column(length=20,nullable=false) public String phone="";
 @Column(length=20,nullable=false) public String provider="";
 public String getId(){return id;}
 protected Account() {}
 public Account(String name,String email,String passwordHash) {this.name=name;this.email=email;this.passwordHash=passwordHash;}
}
