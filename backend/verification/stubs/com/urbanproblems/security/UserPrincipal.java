package com.urbanproblems.security;
public class UserPrincipal { private final Long id; private final String role; private final Long dept;
  public UserPrincipal(Long id, String email, String hash, String role, String type, Long dept, boolean enabled){this.id=id;this.role=role;this.dept=dept;}
  public Long getId(){return id;} public String getRole(){return role;} public Long getDepartmentId(){return dept;} }
