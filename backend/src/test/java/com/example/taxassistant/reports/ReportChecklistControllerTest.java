package com.example.taxassistant.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.checklist.ChecklistItemRepository;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.file.UploadedFileParseErrorRepository;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.report.TaxReportRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.domain.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class ReportChecklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChecklistItemRepository checklistItemRepository;

    @Autowired
    private TaxReportRepository taxReportRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

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

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void reportsSummarizeMonthlyAndYearlyData() throws Exception {
        AuthFixture fixture = createUserBusinessAndTransactions("report-owner@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/reports/monthly", fixture.businessId())
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.reports.length()").value(12))
                .andExpect(jsonPath("$.reports[0].month").value(1))
                .andExpect(jsonPath("$.reports[0].totalRevenue").value(120000))
                .andExpect(jsonPath("$.reports[0].totalExpense").value(53000))
                .andExpect(jsonPath("$.reports[0].expectedIncome").value(67000))
                .andExpect(jsonPath("$.reports[0].unclassifiedTransactionCount").value(1))
                .andExpect(jsonPath("$.reports[0].missingEvidenceTransactionCount").value(1))
                .andExpect(jsonPath("$.reports[0].categoryExpenses[0].categoryName").value("소모품비"))
                .andExpect(jsonPath("$.reports[0].notice").value(containsString("참고용 결과")));

        mockMvc.perform(get("/api/businesses/{businessId}/reports/yearly", fixture.businessId())
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.year").value(2026))
                .andExpect(jsonPath("$.report.totalRevenue").value(150000))
                .andExpect(jsonPath("$.report.totalExpense").value(53000))
                .andExpect(jsonPath("$.report.expectedIncome").value(97000))
                .andExpect(jsonPath("$.report.suspiciousItems.length()").value(1))
                .andExpect(jsonPath("$.report.suspiciousItems[0].differenceAmount").value(30000));

        assertThat(taxReportRepository.findAll()).hasSize(13);
    }

    @Test
    void checklistGeneratesReviewItems() throws Exception {
        AuthFixture fixture = createUserBusinessAndTransactions("checklist-owner@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/checklist", fixture.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.dangerCount").value(1))
                .andExpect(jsonPath("$.warningCount").value(2))
                .andExpect(jsonPath("$.normalCount").value(0))
                .andExpect(jsonPath("$.items[0].severity").value("DANGER"))
                .andExpect(jsonPath("$.items[0].itemType").value("MISSING_EVIDENCE"))
                .andExpect(jsonPath("$.items[1].itemType").value("UNCLASSIFIED_TRANSACTION"))
                .andExpect(jsonPath("$.items[2].itemType").value("BUSINESS_INFO_REVIEW"))
                .andExpect(jsonPath("$.notice").value(containsString("최종 신고 책임")));

        assertThat(checklistItemRepository.findAll()).hasSize(3);
    }

    @Test
    void checklistShowsMissingDocumentWhenNoTransactionsExist() throws Exception {
        AuthFixture fixture = createUserAndBusiness("empty-checklist-owner@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/checklist", fixture.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.items[0].itemType").value("MISSING_DOCUMENT"));
    }

    @Test
    void userCannotReadOtherUsersReportsOrChecklist() throws Exception {
        AuthFixture owner = createUserBusinessAndTransactions("report-owner2@example.com");
        String otherToken = signupAndGetToken("report-other@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/reports/yearly", owner.businessId())
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/businesses/{businessId}/checklist", owner.businessId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private AuthFixture createUserBusinessAndTransactions(String email) throws Exception {
        AuthFixture fixture = createUserAndBusiness(email);
        Business business = businessRepository.findById(UUID.fromString(fixture.businessId())).orElseThrow();

        Transaction sales = transaction(
                business,
                LocalDate.of(2026, 1, 2),
                "스마트스토어",
                "온라인 판매 매출",
                "120000",
                TransactionType.INCOME
        );
        sales.applyCategory("매출", ClassificationStatus.USER_CONFIRMED);
        sales.updateEvidenceStatus(EvidenceStatus.PRESENT);

        Transaction supplies = transaction(
                business,
                LocalDate.of(2026, 1, 3),
                "쿠팡",
                "문구류",
                "33000",
                TransactionType.EXPENSE
        );
        supplies.applyCategory("소모품비", ClassificationStatus.AUTO_CLASSIFIED);
        supplies.updateEvidenceStatus(EvidenceStatus.MISSING);

        Transaction unclassified = transaction(
                business,
                LocalDate.of(2026, 1, 4),
                "미확인거래처",
                "내용 확인 필요",
                "20000",
                TransactionType.EXPENSE
        );
        unclassified.applyCategory(null, ClassificationStatus.NEEDS_REVIEW);

        Transaction unknownDeposit = transaction(
                business,
                LocalDate.of(2026, 2, 1),
                "개인입금",
                "대체 입금",
                "30000",
                TransactionType.INCOME
        );
        unknownDeposit.applyCategory("기타입금", ClassificationStatus.USER_CONFIRMED);

        transactionRepository.saveAll(List.of(sales, supplies, unclassified, unknownDeposit));
        return fixture;
    }

    private Transaction transaction(
            Business business,
            LocalDate date,
            String merchantName,
            String description,
            String amount,
            TransactionType transactionType
    ) {
        return new Transaction(
                business,
                null,
                date,
                merchantName,
                description,
                new BigDecimal(amount),
                transactionType
        );
    }

    private AuthFixture createUserAndBusiness(String email) throws Exception {
        String token = signupAndGetToken(email);
        MvcResult businessResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "리포트 테스트 사업자",
                                null,
                                "도매 및 소매업",
                                BusinessIndustryGroup.GROUP_A,
                                false,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("100000000")
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String businessId = objectMapper.readTree(businessResult.getResponse().getContentAsString()).get("id").asText();
        return new AuthFixture(token, businessId);
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "리포트 테스트 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private void cleanDatabase() {
        checklistItemRepository.deleteAll();
        taxReportRepository.deleteAll();
        parseErrorRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        uploadedFileRepository.deleteAll();
        categoryRuleRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record AuthFixture(String token, String businessId) {
    }
}
