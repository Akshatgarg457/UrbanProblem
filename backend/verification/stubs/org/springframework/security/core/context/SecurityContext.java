package org.springframework.security.core.context;
public interface SecurityContext { org.springframework.security.core.Authentication getAuthentication(); void setAuthentication(org.springframework.security.core.Authentication a); }
