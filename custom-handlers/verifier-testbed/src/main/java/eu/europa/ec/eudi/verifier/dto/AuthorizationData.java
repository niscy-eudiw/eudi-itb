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
package eu.europa.ec.eudi.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationData {

  @JsonProperty("transaction_id")
  public String transactionId;

  @JsonProperty("client_id")
  public String clientId;

  @JsonProperty("request_uri")
  public String requestUri;

  @JsonProperty("request_uri_method")
  public String requestUriMethod;

  @Override
  public String toString() {
    return "AuthorizationData{"
        + "transactionId='"
        + transactionId
        + '\''
        + ", clientId='"
        + clientId
        + '\''
        + ", requestUri='"
        + requestUri
        + '\''
        + ", requestUriMethod='"
        + requestUriMethod
        + '\''
        + '}';
  }
}
