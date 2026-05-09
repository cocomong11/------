package com.example.taxassistant.business;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class BusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadedFileParseErrorRepository parseErrorRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

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
    void authenticatedUserCanManageOwnBusinesses() throws Exception {
        String token = signupAndGetToken("owner@example.com");

        BusinessRequest createRequest = new BusinessRequest(
                "좋은가게",
                "123-45-67890",
                "숙박 및 음식점업",
                BusinessIndustryGroup.GROUP_B,
                false,
                LocalDate.of(2025, 3, 1),
                new BigDecimal("100000000")
        );

        MvcResult createResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("좋은가게"))
                .andExpect(jsonPath("$.bookkeepingPrediction.bookkeepingType").value("SIMPLE_CANDIDATE"))
                .andExpect(jsonPath("$.bookkeepingPrediction.notice").value(containsString("입력값 기준 예상 판정")))
                .andReturn();

        String businessId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(businessId));

        mockMvc.perform(get("/api/businesses/{id}", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(businessId));

        BusinessRequest updateRequest = new BusinessRequest(
                "좋은가게 본점",
                "123-45-67890",
                "전문 서비스업",
                BusinessIndustryGroup.GROUP_C,
                true,
                LocalDate.of(2025, 3, 1),
                new BigDecimal("1000")
        );

        mockMvc.perform(patch("/api/businesses/{id}", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("좋은가게 본점"))
                .andExpect(jsonPath("$.bookkeepingPrediction.bookkeepingType").value("DOUBLE_ENTRY_REQUIRED"))
                .andExpect(jsonPath("$.bookkeepingPrediction.message").value(containsString("전문직 사업자")));
    }

    @Test
    void userCannotReadOtherUsersBusiness() throws Exception {
        String ownerToken = signupAndGetToken("owner@example.com");
        String otherToken = signupAndGetToken("other@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "소유자 가게",
                                null,
                                "도매 및 소매업",
                                BusinessIndustryGroup.GROUP_A,
                                false,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("200000000")
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String businessId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/businesses/{id}", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void businessEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/businesses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "테스트 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
