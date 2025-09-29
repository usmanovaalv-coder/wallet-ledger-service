package com.example.walletledgerservice;

import com.example.walletledgerservice.api.dto.CreateAccountRequest;
import com.example.walletledgerservice.api.dto.TransferRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TransferControllerIT extends IntegrationTestBase {

    static final String ACCOUNT_URL = "/api/v1/accounts";
    static final String TRANSFER_URL = "/api/v1/transfers";
    static final String DEV_MINT_URL  = "/api/v1/dev/treasury/mint/usd";

    static final String HEADER_NAME = "Idempotency-Key";
    static final String USED_CURRENCY = "USD";

    @Test
    void transfer_ok_returns201_andUpdatesBalances() throws Exception {
        long fromId = createAccount(101L, USED_CURRENCY);
        long toId   = createAccount(202L, USED_CURRENCY);

        mintUsd(fromId, 1_000L);

        long beforeFrom = getBalance(fromId);
        long beforeTo   = getBalance(toId);

        TransferRequest req = new TransferRequest(fromId, toId, 500L, USED_CURRENCY, "test transfer", null);

        postJson(TRANSFER_URL, req, idemHeader())
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        long afterFrom = getBalance(fromId);
        long afterTo   = getBalance(toId);

        assertThat(afterFrom).isEqualTo(beforeFrom - 500L);
        assertThat(afterTo).isEqualTo(beforeTo + 500L);
    }

    @Test
    void transfer_idempotent_sameKey_noDoublePosting() throws Exception {
        long fromId = createAccount(111L, USED_CURRENCY);
        long toId   = createAccount(222L, USED_CURRENCY);

        TransferRequest body = new TransferRequest(fromId, toId, 700L, USED_CURRENCY, "test transfer", null);
        mintUsd(fromId, 1_000L);

        String key = UUID.randomUUID().toString();
        postJson(TRANSFER_URL, body, Map.of(HEADER_NAME, key))
                .andExpect(status().isCreated());

        long f1 = getBalance(fromId);
        long t1 = getBalance(toId);

        postJson(TRANSFER_URL, body, Map.of(HEADER_NAME, key))
                .andExpect(status().isCreated());

        long f2 = getBalance(fromId);
        long t2 = getBalance(toId);

        assertThat(f2).isEqualTo(f1);
        assertThat(t2).isEqualTo(t1);
    }

    @Test
    void transfer_insufficientFunds_returns400() throws Exception {
        long fromId = createAccount(333L, USED_CURRENCY);
        long toId   = createAccount(444L, USED_CURRENCY);

        TransferRequest body = new TransferRequest(fromId, toId, 1_000L, USED_CURRENCY, "test transfer", null);

        postJson(TRANSFER_URL, body, idemHeader())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").value("Insufficient funds"));

        assertThat(getBalance(fromId)).isZero();
        assertThat(getBalance(toId)).isZero();
    }

    @Test
    void transfer_missingIdempotencyHeader_returns400() throws Exception {
        long fromId = createAccount(555L, USED_CURRENCY);
        long toId   = createAccount(666L, USED_CURRENCY);

        TransferRequest body = new TransferRequest(fromId, toId, 100L, USED_CURRENCY, "test transfer", null);

        postJson(TRANSFER_URL, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
    }

    @Test
    void transfer_currencyMismatch_returns400() throws Exception {
        long fromId = createAccount(777L, USED_CURRENCY);
        long toId   = createAccount(888L, "EUR");

        TransferRequest body = new TransferRequest(fromId, toId, 50L, USED_CURRENCY, "test transfer", null);

        postJson(TRANSFER_URL, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private long createAccount(long ownerId, String currencyUpperOrLower) throws Exception {
        var body = new CreateAccountRequest(ownerId, currencyUpperOrLower);
        MvcResult res = postJson(ACCOUNT_URL, body)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private long getBalance(long accountId) throws Exception {
        MvcResult res = getJson(ACCOUNT_URL + "/{id}", accountId)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("balanceMinor").asLong();
    }

    private void mintUsd(long fromId, long amountMinor) throws Exception {
        Map<String, String> param = new HashMap<>();
        param.put("toAccountId", String.valueOf(fromId));
        param.put("amountMinor", String.valueOf(amountMinor));

        postJson(DEV_MINT_URL, idemHeader(), param)
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

    }

    private Map<String,String> idemHeader() {
        return Map.of(HEADER_NAME, UUID.randomUUID().toString());
    }
}
