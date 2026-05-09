package com.example.taxassistant.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.file.UploadedFileParseErrorRepository;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.domain.user.UserRepository;
import com.example.taxassistant.transactions.dto.TransactionUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UploadedFileParseErrorRepository parseErrorRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void setUp() {
        parseErrorRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        uploadedFileRepository.deleteAll();
        categoryRuleRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void classifyTransactionsAndSavePersonalRuleOnUserUpdate() throws Exception {
        AuthFixture fixture = createUserBusinessAndTransactions("classification-owner@example.com");

        MvcResult classifyResult = mockMvc.perform(post("/api/businesses/{businessId}/classify-transactions", fixture.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.autoClassifiedCount").value(1))
                .andExpect(jsonPath("$.needsReviewCount").value(2))
                .andReturn();
        JsonNode classifiedTransactions = objectMapper.readTree(classifyResult.getResponse().getContentAsString())
                .get("transactions");
        assertThat(findByMerchant(classifiedTransactions, "쿠팡").get("categoryName").asText()).isEqualTo("소모품비");
        assertThat(findByMerchant(classifiedTransactions, "국민연금").get("classificationStatus").asText())
                .isEqualTo("NEEDS_REVIEW");

        MvcResult listResult = mockMvc.perform(get("/api/businesses/{businessId}/transactions", fixture.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode transactions = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String unknownTransactionId = null;
        for (JsonNode transaction : transactions) {
            if ("미상거래처".equals(transaction.get("merchantName").asText())) {
                unknownTransactionId = transaction.get("id").asText();
            }
        }

        mockMvc.perform(patch("/api/transactions/{id}", unknownTransactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionUpdateRequest(
                                "접대비",
                                "사용자 확인 완료"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("접대비"))
                .andExpect(jsonPath("$.classificationStatus").value("USER_CONFIRMED"));

        var savedTransaction = transactionRepository.findById(UUID.fromString(unknownTransactionId)).orElseThrow();
        assertThat(categoryRuleRepository.findFirstByBusinessIdAndKeywordIgnoreCaseAndActiveTrue(
                savedTransaction.getBusiness().getId(),
                "미상거래처"
        ).orElseThrow().getCategoryName()).isEqualTo("접대비");
    }

    @Test
    void userCannotClassifyOtherUsersBusiness() throws Exception {
        AuthFixture owner = createUserBusinessAndTransactions("classification-owner2@example.com");
        String otherToken = signupAndGetToken("classification-other@example.com");

        mockMvc.perform(post("/api/businesses/{businessId}/classify-transactions", owner.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void transactionUpdateRequiresOwnership() throws Exception {
        AuthFixture owner = createUserBusinessAndTransactions("transaction-owner@example.com");
        String otherToken = signupAndGetToken("transaction-other@example.com");
        MvcResult listResult = mockMvc.perform(get("/api/businesses/{businessId}/transactions", owner.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andReturn();
        String transactionId = objectMapper.readTree(listResult.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(patch("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionUpdateRequest("소모품비", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private AuthFixture createUserBusinessAndTransactions(String email) throws Exception {
        String token = signupAndGetToken(email);
        MvcResult businessResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "분류 테스트 사업자",
                                null,
                                "도매 및 소매업",
                                BusinessIndustryGroup.GROUP_A,
                                false,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("100000000")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookkeepingPrediction.notice").value(containsString("입력값 기준 예상 판정")))
                .andReturn();
        String businessId = objectMapper.readTree(businessResult.getResponse().getContentAsString()).get("id").asText();

        String csv = """
                거래일자,거래처,내용,입금액,출금액,부가세
                2026-05-01,쿠팡,문구류,,33000,3000
                2026-05-02,국민연금,4대보험,,120000,0
                2026-05-03,미상거래처,회의비,,50000,0
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/businesses/{businessId}/files", businessId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        return new AuthFixture(token, businessId);
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "분류 테스트 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode findByMerchant(JsonNode transactions, String merchantName) {
        for (JsonNode transaction : transactions) {
            if (merchantName.equals(transaction.get("merchantName").asText())) {
                return transaction;
            }
        }
        throw new AssertionError("Transaction not found: " + merchantName);
    }

    private record AuthFixture(String token, String businessId) {
    }
}
