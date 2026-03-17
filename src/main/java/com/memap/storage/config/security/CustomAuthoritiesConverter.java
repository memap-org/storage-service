package com.memap.storage.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  @SuppressWarnings("unchecked")
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);

    if (realmAccess == null || realmAccess.isEmpty()) {
      return Collections.emptyList();
    }

    Object rolesObj = realmAccess.get(ROLES_CLAIM);
    if (!(rolesObj instanceof List)) {
      return Collections.emptyList();
    }

    List<String> roles = (List<String>) rolesObj;

    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
        .collect(Collectors.toList());
  }
}
