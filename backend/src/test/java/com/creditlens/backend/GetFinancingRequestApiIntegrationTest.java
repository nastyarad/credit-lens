package com.creditlens.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.support.BackendApiIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetFinancingRequestApiIntegrationTest extends BackendApiIntegrationTestSupport {

  @Test
  void getsPersistedRequestDetailsWithCompleteExtractAndMaskedIdentityCode() throws Exception {
    stubPcrResponse("pcr-complete-extract.json");

    String location =
        mockMvc
            .perform(
                post("/api/v1/financing-requests")
                    .contentType("application/json")
                    .content(createRequest(UUID.randomUUID(), "010190-123A", "NewConsumerCredit")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    String response =
        mockMvc
            .perform(get(location))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.consumer.maskedPersonalIdentityCode").value("******-123A"))
            .andExpect(
                jsonPath("$.creditExtract.extractReference")
                    .value("55555555-5555-5555-5555-555555555555"))
            .andExpect(
                jsonPath(
                        "$.creditExtract.creditInformationSummary.repaymentsPaidLastAmount[0].currencyCode")
                    .value("EUR"))
            .andExpect(
                jsonPath(
                        "$.creditExtract.creditInformationSummary.sumOfMonthlyLeasingInstalments[0].sum")
                    .value(250.0))
            .andExpect(jsonPath("$.creditExtract.loans[0].loanType").value("LumpSumLoan"))
            .andExpect(jsonPath("$.creditExtract.loans[0].lumpSumLoan.balance").value(8500))
            .andExpect(
                jsonPath("$.creditExtract.loans[0].delayedAmount[0].isForeclosed").value(false))
            .andExpect(
                jsonPath("$.creditExtract.incomeData[0].months[0].wagesNetAmount").value(3000))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("010190-123A");
    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
  }
}
