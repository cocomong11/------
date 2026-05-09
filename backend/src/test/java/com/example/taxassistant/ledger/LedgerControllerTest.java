package com.example.taxassistant.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        parseErrorRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        uploadedFileRepository.deleteAll();
        categoryRuleRepository.deleteAll();
        businessRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsLedgerFromTransactionsForMonth() throws Exception {
        AuthFixture fixture = createUserBusinessAndTransactions("ledger-owner@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/ledger", fixture.businessId())
                        .param("year", "2026")
                        .param("month", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.summary.totalRevenue").value(100000))
                .andExpect(jsonPath("$.summary.totalExpense").value(33000))
                .andExpect(jsonPath("$.summary.netIncome").value(67000))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].accountTitle").value("소모품비"))
                .andExpect(jsonPath("$.notice").value("사용자 입력 및 업로드 자료 기준의 참고용 결과이며, 최종 신고 책임은 사용자에게 있음"));

        assertThat(ledgerEntryRepository.findAllByBusinessId(java.util.UUID.fromString(fixture.businessId())))
                .hasSize(2);
    }

    @Test
    void exportsLedgerExcelWithTotalRow() throws Exception {
        AuthFixture fixture = createUserBusinessAndTransactions("ledger-export-owner@example.com");

        MvcResult result = mockMvc.perform(get("/api/businesses/{businessId}/exports/ledger.xlsx", fixture.businessId())
                        .param("year", "2026")
                        .param("month", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("ledger-2026-01.xlsx")))
                .andReturn();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("2026년 1월");
            assertThat(sheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("소모품비");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("합계");
            assertThat(sheet.getRow(6).getCell(3).getNumericCellValue()).isEqualTo(100000.0);
            assertThat(sheet.getRow(6).getCell(4).getNumericCellValue()).isEqualTo(33000.0);
        }
    }

    @Test
    void userCannotReadOtherUsersLedger() throws Exception {
        AuthFixture owner = createUserBusinessAndTransactions("ledger-owner2@example.com");
        String otherToken = signupAndGetToken("ledger-other@example.com");

        mockMvc.perform(get("/api/businesses/{businessId}/ledger", owner.businessId())
                        .param("year", "2026")
                        .param("month", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private AuthFixture createUserBusinessAndTransactions(String email) throws Exception {
        String token = signupAndGetToken(email);
        MvcResult businessResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "장부 테스트 사업자",
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

        String csv = """
                거래일자,거래처,내용,입금액,출금액,부가세
                2026-01-02,쿠팡,문구류,,33000,3000
                2026-01-03,네이버페이,판매대금,100000,,0
                2026-02-03,다이소,다른 달 거래,,5000,500
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ledger-transactions.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/businesses/{businessId}/files", businessId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/businesses/{businessId}/classify-transactions", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        return new AuthFixture(token, businessId);
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "장부 테스트 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private record AuthFixture(String token, String businessId) {
    }
}
