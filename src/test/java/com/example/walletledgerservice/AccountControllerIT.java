package com.example.walletledgerservice;

import com.example.walletledgerservice.api.dto.CreateAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AccountControllerIT extends IntegrationTestBase {

    static final String ACCOUNT_URL = "/api/v1/accounts";
    static final Long OWNER_ID = 42L;
    static final String USD_CURRENCY = "USD";

    @Test
    void create_returns201_andBody() throws Exception {
        var body = new CreateAccountRequest(OWNER_ID, "usd");

        postJson(ACCOUNT_URL, body)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$.currency").value(USD_CURRENCY))
                .andExpect(jsonPath("$.balanceMinor").value(0));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        createAccount(OWNER_ID, USD_CURRENCY);

        postJson(ACCOUNT_URL, new CreateAccountRequest(OWNER_ID, USD_CURRENCY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void get_returns200_andBody() throws Exception {
        long id = createAccount(OWNER_ID, USD_CURRENCY);

        getJson(ACCOUNT_URL + "/{id}", id)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$.currency").value(USD_CURRENCY))
                .andExpect(jsonPath("$.balanceMinor").value(0));

    }

    @Test
    void get_notFound_returns404() throws Exception {
        getJson(ACCOUNT_URL + "/{id}", Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private long createAccount(Long ownerId, String currency) throws Exception {
        var body = new CreateAccountRequest(ownerId, currency);
        var res = postJson(ACCOUNT_URL, body)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }
}
