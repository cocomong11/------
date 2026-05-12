package com.example.taxassistant.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxassistant.auth.dto.LoginRequest;
import com.example.taxassistant.auth.dto.RefreshTokenRequest;
import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.auth.dto.VerifyEmailRequest;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessVerificationRequest;
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
@SpringBootTest(properties = {
        "app.security.email.auto-verify=false",
        "app.security.email.fixed-code=123456"
})
@AutoConfigureMockMvc
class AuthTrustFlowControllerTest {

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
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

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
    void emailVerificationIsRequiredBeforeLoginAndSucceedsWithCode() throws Exception {
        signup("pending-owner@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "pending-owner@example.com",
                                "password123"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(
                                "pending-owner@example.com",
                                "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void refreshTokenCanBeRotatedAndLogoutRevokesIt() throws Exception {
        String refreshToken = signupAndVerify("refresh-owner@example.com").get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refreshToken")
                .asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void accountIsLockedAfterFiveFailedLoginAttempts() throws Exception {
        signupAndVerify("locked-owner@example.com");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(
                                    "locked-owner@example.com",
                                    "wrong-password"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "locked-owner@example.com",
                                "wrong-password"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "locked-owner@example.com",
                                "password123"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void businessVerificationReturnsSuccessAndFailureStatuses() throws Exception {
        String token = signupAndVerify("business-verification-owner@example.com").get("accessToken").asText();
        String businessId = createBusiness(token);

        mockMvc.perform(post("/api/businesses/{businessId}/verify", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessVerificationRequest(
                                "123-45-67890",
                                "검증 사용자",
                                LocalDate.of(2025, 1, 1)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        mockMvc.perform(post("/api/businesses/{businessId}/verify", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessVerificationRequest(
                                "123-45-00000",
                                "검증 사용자",
                                LocalDate.of(2025, 1, 1)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("FAILED"));
    }

    @Test
    void agreementIsRequiredBeforeBusinessRegistration() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "미동의 사용자",
                                "no-agreement@example.com",
                                "password123",
                                false,
                                false,
                                false,
                                false,
                                false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        assert signupBody.get("accessToken").isNull();

        String token = verifyEmail("no-agreement@example.com").get("accessToken").asText();

        mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AGREEMENT_REQUIRED"));
    }

    private void signup(String email) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "검증 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    private JsonNode signupAndVerify(String email) throws Exception {
        signup(email);
        return verifyEmail(email);
    }

    private JsonNode verifyEmail(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(email, "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createBusiness(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(businessRequest())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private BusinessRequest businessRequest() {
        return new BusinessRequest(
                "검증 테스트 사업자",
                "123-45-67890",
                "검증 사용자",
                "도매 및 소매업",
                "일반과세자",
                BusinessIndustryGroup.GROUP_A,
                false,
                false,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("100000000")
        );
    }
}
