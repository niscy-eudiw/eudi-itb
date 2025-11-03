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
package eu.europa.ec.eudi.verifier.service

import com.gitb.core.AnyContent
import com.gitb.core.ValueEmbeddingEnumeration
import com.gitb.tr.TestResultType
import com.gitb.tr.ValidationCounters
import com.gitb.vs.GetModuleDefinitionResponse
import com.gitb.vs.ValidateRequest
import com.gitb.vs.ValidationResponse
import com.gitb.vs.ValidationService
import eu.europa.ec.eudi.gitb.Utils
import eu.europa.ec.eudi.verifier.dto.AttestationStatusCheckFailed
import eu.europa.ec.eudi.verifier.dto.FailedToRetrievePresentationDefinition
import eu.europa.ec.eudi.verifier.dto.FailedToRetrieveRequestObject
import eu.europa.ec.eudi.verifier.dto.PresentationEventsTO
import eu.europa.ec.eudi.verifier.dto.PresentationExpired
import eu.europa.ec.eudi.verifier.dto.VerifierFailedToGetWalletResponse
import eu.europa.ec.eudi.verifier.dto.VerifierGotWalletResponse
import eu.europa.ec.eudi.verifier.dto.WalletResponsePosted
import eu.europa.ec.eudi.verifier.utils.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class VerifierValidationService(
    private val utils: Utils,
    @Qualifier("utilsJson")
    private val json: Json,
) : ValidationService {
    private val log: Logger = LoggerFactory.getLogger(VerifierValidationService::class.java)

    override fun getModuleDefinition(parameters: com.gitb.vs.Void?): GetModuleDefinitionResponse = GetModuleDefinitionResponse()

    override fun validate(parameters: ValidateRequest?): ValidationResponse {
        log.info(
            "Received 'validate' command from test bed for session [{}]",
            parameters!!.getSessionId(),
        )

        // First extract the parameters and check to see if they are as expected.
        val providedText = utils.getRequiredString(parameters.getInput(), "text")

        val providedLogs = json.reader.readValue(providedText, PresentationEventsTO::class.java)

        val walletResponseEvent = providedLogs.events.filterIsInstance<WalletResponsePosted>()
        val verifierWalletResponseEvent = providedLogs.events.filterIsInstance<VerifierGotWalletResponse>()

        val verifierQuery = verifierWalletResponseEvent.firstOrNull()?.walletResponse
        val walletQuery = walletResponseEvent.firstOrNull()?.walletResponse

        val successCheck = providedLogs.events.filterIsInstance<VerifierGotWalletResponse>()

        val warnings =
            providedLogs.events.mapNotNull { event ->
                when (event) {
                    is AttestationStatusCheckFailed -> event.cause
                    is FailedToRetrievePresentationDefinition -> event.cause
                    is FailedToRetrieveRequestObject -> event.cause
                    is PresentationExpired -> "Presentation expired"
                    is VerifierFailedToGetWalletResponse -> event.cause
                    else -> null
                }
            }.distinct()
        // This is the only non-recoverable error that can occur
        val nonRecoverableErrors =
            if (verifierQuery != walletQuery) {
                "Wallet query and verifier query do not match"
            } else {
                null
            }

        // Build report with the appropriate result based on errors
        val report =
            utils.createReport(
                if (nonRecoverableErrors != null || successCheck.isEmpty()) {
                    TestResultType.FAILURE
                } else {
                    TestResultType.SUCCESS
                },
            )

        val verifierLogs = providedLogs.toJsonContent("Verifier's Logs")

        report.apply {
            counters = ValidationCounters()
            context.item.add(verifierLogs)
            log.info("nonRecoverableErrors created: {}", nonRecoverableErrors)
            if (nonRecoverableErrors != null) {
                context.item.add(
                    nonRecoverableErrors.toJsonContent("Non-recoverable errors"),
                )
                counters.nrOfErrors = 1.toBigInteger()
            }
            if (warnings.isNotEmpty()) {
                context.item.add(warnings.toStrings().toJsonContent("Validation warnings"))
            }
            counters.nrOfWarnings = warnings.size.toBigInteger()
        }

        return ValidationResponse().apply { this.report = report }
    }

    private fun List<String>.toStrings(): String = this.joinToString(" \n")

    private fun Any.toJsonContent(name: String): AnyContent =
        AnyContent().apply {
            this.name = name
            mimeType = "application/json"
            encoding = "UTF-8"
            item.add(
                utils.createAnyContentSimple(
                    "JSON Data",
                    json.writer.writeValueAsString(this@toJsonContent),
                    ValueEmbeddingEnumeration.STRING,
                ),
            )
        }
}
