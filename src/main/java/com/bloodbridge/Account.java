package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="accounts")
public class Account {
 @Id @Column(length=36) public String id = UUID.randomUUID().toString();
 @Column(nullable=false,length=80) public String name;
 @Column(nullable=false,unique=true,length=180) public String email;
 @Column(name="password_hash",nullable=false,length=100) public String passwordHash;
 protected Account() {}
 public Account(String name,String email,String passwordHash) {this.name=name;this.email=email;this.passwordHash=passwordHash;}
}
