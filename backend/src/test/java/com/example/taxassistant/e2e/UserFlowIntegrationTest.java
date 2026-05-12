package com.example.taxassistant.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxassistant.auth.dto.LoginRequest;
import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.checklist.ChecklistItemRepository;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.file.UploadedFileParseErrorRepository;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.report.TaxReportRepository;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.domain.user.UserRepository;
import com.example.taxassistant.transactions.dto.TransactionUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
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
class UserFlowIntegrationTest {

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
    void userCanCompleteCoreMvpFlow() throws Exception {
        String email = "flow-owner@example.com";
        String password = "password123";

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "흐름 테스트 사용자",
                                email,
                                password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        String signupToken = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        MvcResult businessResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "전체 흐름 테스트 사업자",
                                "123-45-67890",
                                "도매 및 소매업",
                                BusinessIndustryGroup.GROUP_A,
                                false,
                                LocalDate.of(2025, 1, 1),
                                new BigDecimal("120000000")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookkeepingPrediction.bookkeepingType").value("SIMPLE_CANDIDATE"))
                .andExpect(jsonPath("$.bookkeepingPrediction.notice").value(containsString("입력값 기준 예상 판정")))
                .andReturn();
        String businessId = objectMapper.readTree(businessResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(businessId));

        mockMvc.perform(post("/api/businesses/{businessId}/verify", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessVerificationRequest(
                                "123-45-67890",
                                "흐름 테스트 사용자",
                                LocalDate.of(2025, 1, 1)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", businessId)
                        .file(csvFile())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("sample-transactions.csv"))
                .andExpect(jsonPath("$.parsedCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0));

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", businessId)
                        .file(xlsxFile())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("sample-transactions.xlsx"))
                .andExpect(jsonPath("$.parsedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0));

        MvcResult classifyResult = mockMvc.perform(post("/api/businesses/{businessId}/classify-transactions", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.autoClassifiedCount").value(2))
                .andExpect(jsonPath("$.needsReviewCount").value(2))
                .andReturn();
        JsonNode classifiedTransactions = objectMapper.readTree(classifyResult.getResponse().getContentAsString())
                .get("transactions");
        String salesTransactionId = findTransactionIdByMerchant(classifiedTransactions, "스마트스토어");

        mockMvc.perform(patch("/api/transactions/{id}", salesTransactionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionUpdateRequest(
                                "매출",
                                "매출 거래로 확인"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("매출"))
                .andExpect(jsonPath("$.classificationStatus").value("USER_CONFIRMED"))
                .andExpect(jsonPath("$.userMemo").value("매출 거래로 확인"));

        mockMvc.perform(get("/api/businesses/{businessId}/ledger", businessId)
                        .param("year", "2026")
                        .param("month", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalRevenue").value(150000))
                .andExpect(jsonPath("$.summary.totalExpense").value(88000))
                .andExpect(jsonPath("$.summary.netIncome").value(62000))
                .andExpect(jsonPath("$.entries.length()").value(4))
                .andExpect(jsonPath("$.notice").value(containsString("참고용 결과")));

        mockMvc.perform(get("/api/businesses/{businessId}/reports/monthly", businessId)
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports.length()").value(12))
                .andExpect(jsonPath("$.reports[0].totalRevenue").value(150000))
                .andExpect(jsonPath("$.reports[0].totalExpense").value(88000))
                .andExpect(jsonPath("$.reports[0].unclassifiedTransactionCount").value(1));

        mockMvc.perform(get("/api/businesses/{businessId}/reports/yearly", businessId)
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.totalRevenue").value(150000))
                .andExpect(jsonPath("$.report.totalExpense").value(88000))
                .andExpect(jsonPath("$.report.expectedIncome").value(62000))
                .andExpect(jsonPath("$.report.suspiciousItems.length()").value(1))
                .andExpect(jsonPath("$.report.suspiciousItems[0].differenceAmount").value(30000));

        mockMvc.perform(get("/api/businesses/{businessId}/checklist", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.warningCount").value(2))
                .andExpect(jsonPath("$.dangerCount").value(0))
                .andExpect(jsonPath("$.items[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.notice").value(containsString("최종 신고 책임")));

        MvcResult excelResult = mockMvc.perform(get("/api/businesses/{businessId}/exports/ledger.xlsx", businessId)
                        .param("year", "2026")
                        .param("month", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("ledger-2026-01.xlsx")))
                .andReturn();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelResult.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("2026년 1월");
            assertThat(sheet.getRow(8).getCell(0).getStringCellValue()).isEqualTo("합계");
            assertThat(sheet.getRow(8).getCell(3).getNumericCellValue()).isEqualTo(150000.0);
            assertThat(sheet.getRow(8).getCell(4).getNumericCellValue()).isEqualTo(88000.0);
        }

        assertThat(transactionRepository.findAll()).hasSize(4);
        assertThat(ledgerEntryRepository.findAll()).hasSize(4);
        assertThat(checklistItemRepository.findAll()).hasSize(2);
    }

    private MockMultipartFile csvFile() {
        String csv = """
                거래일자,거래처,내용,입금액,출금액,부가세
                2026-01-02,스마트스토어,온라인 판매 매출,120000,,0
                2026-01-03,쿠팡,문구류,,33000,3000
                2026-01-04,개인입금,사업 외 입금,30000,,0
                """;
        return new MockMultipartFile(
                "file",
                "sample-transactions.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile xlsxFile() throws Exception {
        return new MockMultipartFile(
                "file",
                "sample-transactions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes()
        );
    }

    private byte[] xlsxBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("transactions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("date");
            header.createCell(1).setCellValue("vendor");
            header.createCell(2).setCellValue("memo");
            header.createCell(3).setCellValue("income");
            header.createCell(4).setCellValue("expense");
            header.createCell(5).setCellValue("vat");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-01-05");
            row.createCell(1).setCellValue("네이버광고");
            row.createCell(2).setCellValue("검색광고");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue(55000);
            row.createCell(5).setCellValue(5000);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String findTransactionIdByMerchant(JsonNode transactions, String merchantName) {
        for (JsonNode transaction : transactions) {
            if (merchantName.equals(transaction.get("merchantName").asText())) {
                return transaction.get("id").asText();
            }
        }
        throw new AssertionError("Transaction not found: " + merchantName);
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
}
