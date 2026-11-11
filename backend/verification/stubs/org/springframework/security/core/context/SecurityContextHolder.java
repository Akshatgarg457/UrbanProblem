package org.springframework.security.core.context;
public class SecurityContextHolder {
  private static SecurityContext ctx = new SecurityContext() {
    private org.springframework.security.core.Authentication a;
    public org.springframework.security.core.Authentication getAuthentication(){return a;}
    public void setAuthentication(org.springframework.security.core.Authentication x){a=x;} };
  public static SecurityContext getContext(){ return ctx; }
}
