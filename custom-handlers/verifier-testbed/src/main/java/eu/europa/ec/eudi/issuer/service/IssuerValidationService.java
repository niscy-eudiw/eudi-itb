/*
 * Copyright (c) 2025-2026 European Commission
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
package eu.europa.ec.eudi.issuer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.vs.*;
import com.gitb.vs.Void;
import eu.europa.ec.eudi.gitb.Utils;
import eu.europa.ec.eudi.issuer.dto.CredentialOfferLogsTO;
import eu.europa.ec.eudi.verifier.utils.Json;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IssuerValidationService implements ValidationService {
  @Autowired private Utils utils;
  @Autowired private Json json;

  private Logger log = LoggerFactory.getLogger(IssuerValidationService.class);

  @Override
  public GetModuleDefinitionResponse getModuleDefinition(Void parameters) {
    return new GetModuleDefinitionResponse();
  }

  @Override
  public ValidationResponse validate(ValidateRequest parameters) {
    log.info(
        "Received 'validate' command from test bed for session [{}]", parameters.getSessionId());

    TAR report = utils.createReport(TestResultType.SUCCESS);

    String providedText = utils.getRequiredString(parameters.getInput(), "text");
    log.info("Retrieved issuer's logs from 'input' text.");

    String expectedText = null;
    try {
      expectedText = utils.getRequiredString(parameters.getInput(), "expected");
      log.info("Retrieved 'expected' text.");
    } catch (Exception e) {
      log.warn("None 'expected' text was received. Exception Message: {}", e.getMessage());
    }

    CredentialOfferLogsTO providedLogs;
    try {
      providedLogs = json.getReader().readValue(providedText, CredentialOfferLogsTO.class);
      log.info("Loaded issuer's logs into CredentialOfferLogs Object.");
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to load issuer's logs into  CredentialOfferLogs Object. Exception Message: {}",
          e.getMessage());
      throw new RuntimeException(e);
    }

    if (providedLogs.getSuccessful()) {
      report.setResult(TestResultType.SUCCESS);
    } else {
      report.setResult(TestResultType.FAILURE);
    }
    log.info("Added test result type to Report.");

    AnyContent logs;
    try {
      ObjectNode logsJSON = fromListToJSONArray(providedLogs);
      log.info("Created JSON Array from list of issuer's logs.");
      logs = toContent(logsJSON);
      report.getContext().getItem().add(logs);
      log.info("Added issuer's logs to Report.");
    } catch (JsonProcessingException e) {
      log.error("Failed to add issuer's log to Report. Exception Message: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    ValidationResponse result = new ValidationResponse();
    result.setReport(report);
    return result;
  }

  private void debugMatch(String name, String regex, String logLine) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(logLine);

    if (!m.find()) {
      log.error("{} not found in log.", name);
      return;
    }

    log.info("{} found in: '{}'", name, m.group(0));
    int groupCount = m.groupCount();
    for (int i = 1; i <= groupCount; i++) {
      try {
        log.info("  group({}): '{}'", i, m.group(i));
      } catch (Exception e) {
        log.warn("  Failed to retrieve group({}). Exception: {}", i, e.getMessage());
      }
    }
  }

  private ObjectNode fromListToJSONArray(CredentialOfferLogsTO logs) {
    int info_counter_logs = 0;
    int warn_counter_logs = 0;
    int error_counter_logs = 0;

    ObjectNode logsJsonObject = this.json.getReader().createObjectNode();

    ArrayNode json = this.json.getReader().createArrayNode();
    for (String logLine : logs.getLogs()) {
      debugMatch("Timestamp", "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}).*", logLine);
      debugMatch("Logger name", "^\\s*\\S+\\s+(\\S+).*", logLine);
      debugMatch("Level", ".*\\b(INFO|WARN|ERROR|DEBUG|TRACE)\\b.*", logLine);
      log.info("Checked if log matches expected format: {}", logLine);

      Pattern p =
          Pattern.compile(
              "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})\\s+([\\w\\.]+)\\s+(INFO|WARN|ERROR|DEBUG|TRACE)\\s+(?:,\\s*)?(.*)$");
      Matcher m = p.matcher(logLine);

      if (m.find()) {
        ObjectNode logAsJSON = this.json.getReader().createObjectNode();
        logAsJSON.put("timestamp", m.group(1));
        logAsJSON.put("logger", m.group(2));
        logAsJSON.put("level", m.group(3));
        switch (m.group(3)) {
          case "INFO" -> info_counter_logs++;
          case "WARN" -> warn_counter_logs++;
          case "ERROR" -> error_counter_logs++;
        }
        logAsJSON.put("message", m.group(4));
        logAsJSON.put("full_log", logLine);
        json.add(logAsJSON);
        log.debug(logAsJSON.toString());
      } else {
        log.warn(
            "Failed to retrieved required information (timestamp, logger name, level) from log {}",
            logLine);
      }
    }

    logsJsonObject.set("logs", json);

    ObjectNode counter = this.json.getReader().createObjectNode();
    counter.put("error_count", error_counter_logs);
    counter.put("warn_count", warn_counter_logs);
    counter.put("info_count", info_counter_logs);
    counter.put("total_count", logs.getCount());

    logsJsonObject.set("log_stats", counter);

    return logsJsonObject;
  }

  private AnyContent toContent(ObjectNode logs) throws JsonProcessingException {
    log.info("Adding logs to Result.");
    log.debug("Logs: {}", logs.toString());

    AnyContent result = new AnyContent();
    result.setName("Issuer's Logs");
    result.setType("application/json");
    result.setEncoding("UTF-8");
    result
        .getItem()
        .add(
            utils.createAnyContentSimple(
                "JSON Data",
                json.getWriter().writeValueAsString(logs),
                ValueEmbeddingEnumeration.STRING));
    return result;
  }
}
