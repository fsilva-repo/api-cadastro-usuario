package com.register.api.model;

import java.security.AuthProvider;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

  @Id
  private String id;       // gerado pelo MongoDB
  private String name;
  @Indexed(unique = true)
  private String email;
  private String passwordHash;  // null para contas Google
  private AuthProvider provider;
  private String providerId;
  private Set<String> roles = new HashSet<>();
  private Instant createdAt = Instant.now();

  // usado pelo Spring Data ao ler do banco
  protected User() {}

  // criar um novo usuário (sem id e sem createdAt)
  public User(String name, String email, String passwordHash,
      AuthProvider provider, String providerId, Set<String> roles) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.provider = provider;
    this.providerId = providerId;
    this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    this.createdAt = Instant.now();
  }

  public String getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public AuthProvider getProvider() { return provider; }
  public String getProviderId() { return providerId; }
  public Set<String> getRoles() { return roles; }
  public Instant getCreatedAt() { return createdAt; }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    User other = (User) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  
  

}
