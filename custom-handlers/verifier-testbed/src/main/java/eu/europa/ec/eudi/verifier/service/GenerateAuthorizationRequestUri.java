/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.europa.ec.eudi.verifier.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GenerateAuthorizationRequestUri {
  public static class JwtSecuredAuthorizationRequestTO {
    public String clientId;
    public String request;
    public String requestUri;
    public RequestUriMethodTO requestUriMethod;
  }

  public enum RequestUriMethodTO {
    Get,
    Post
  }

  public static URI createAuthorizationRequestUri(
      String scheme, JwtSecuredAuthorizationRequestTO authorizationRequest) {
    String customDomainAndPath = "";

    Objects.requireNonNull(scheme, "scheme must not be null");
    Objects.requireNonNull(authorizationRequest, "authorizationRequest must not be null");
    Objects.requireNonNull(
        authorizationRequest.clientId, "authorizationRequest.clientId must not be null");

    // Build query parameters in insertion order
    Map<String, String> params = new LinkedHashMap<>();

    params.put("client_id", authorizationRequest.clientId);

    if (authorizationRequest.request != null) {
      params.put("request", authorizationRequest.request);
    }
    if (authorizationRequest.requestUri != null) {
      params.put("request_uri", authorizationRequest.requestUri);
    }
    if (authorizationRequest.requestUriMethod != null) {
      params.put("request_uri_method", "get");
    }

    String query = buildQuery(params);

    return URI.create(scheme + "://" + customDomainAndPath + "?" + query);
  }

  private static String buildQuery(Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> e : params.entrySet()) {
      if (!first) sb.append('&');
      first = false;
      sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
    }
    return sb.toString();
  }

  private static String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
