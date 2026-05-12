package com.example.taxassistant.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxassistant.auth.dto.SignupRequest;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.file.UploadedFileParseErrorRepository;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.domain.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class FileUploadControllerTest {

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
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

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
    void uploadCsvParsesTransactionsAndStoresRowErrors() throws Exception {
        AuthFixture fixture = createUserAndBusiness("csv-owner@example.com");
        String csv = """
                거래일자,거래처,내용,입금액,출금액,부가세
                2026-05-01,쿠팡,문구류,,33000,3000
                2026-05-02,네이버페이,판매대금,120000,,0
                날짜오류,다이소,소모품,,10000,1000
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/businesses/{businessId}/files", fixture.businessId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("transactions.csv"))
                .andExpect(jsonPath("$.processingStatus").value("PARSED"))
                .andExpect(jsonPath("$.parsedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.errors[0].rowNumber").value(4))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("거래일자")))
                .andReturn();

        String fileId = objectMapper.readTree(result.getResponse().getContentAsString()).get("fileId").asText();
        assertThat(transactionRepository.findAllByUploadedFileId(UUID.fromString(fileId))).hasSize(2);
        assertThat(parseErrorRepository.countByUploadedFileId(UUID.fromString(fileId))).isEqualTo(1);
        assertThat(transactionRepository.findAllByUploadedFileId(UUID.fromString(fileId))
                .stream()
                .anyMatch(transaction -> transaction.getTransactionType() == TransactionType.INCOME
                        && transaction.getAmount().compareTo(new BigDecimal("120000")) == 0))
                .isTrue();
    }

    @Test
    void uploadXlsxParsesTransactions() throws Exception {
        AuthFixture fixture = createUserAndBusiness("xlsx-owner@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes()
        );

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", fixture.businessId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void uploadRejectsUnsupportedExtension() throws Exception {
        AuthFixture fixture = createUserAndBusiness("invalid-file-owner@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", fixture.businessId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void uploadRejectsOversizedFile() throws Exception {
        AuthFixture fixture = createUserAndBusiness("large-file-owner@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                new byte[10_485_761]
        );

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", fixture.businessId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void uploadRequiresBusinessOwnership() throws Exception {
        AuthFixture owner = createUserAndBusiness("file-owner@example.com");
        String otherToken = signupAndGetToken("file-other@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                "거래일자,입금액\n2026-05-01,1000\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/businesses/{businessId}/files", owner.businessId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/businesses/00000000-0000-0000-0000-000000000000/files"))
                .andExpect(status().isUnauthorized());
    }

    private AuthFixture createUserAndBusiness(String email) throws Exception {
        String token = signupAndGetToken(email);
        MvcResult businessResult = mockMvc.perform(post("/api/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessRequest(
                                "파일 테스트 사업자",
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
        verifyBusiness(token, businessId);
        return new AuthFixture(token, businessId);
    }

    private void verifyBusiness(String token, String businessId) throws Exception {
        mockMvc.perform(post("/api/businesses/{businessId}/verify", businessId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BusinessVerificationRequest(
                                "123-45-67890",
                                "파일 테스트 사용자",
                                LocalDate.of(2025, 1, 1)
                        ))))
                .andExpect(status().isOk());
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "파일 테스트 사용자",
                                email,
                                "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private byte[] xlsxBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("transactions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("날짜");
            header.createCell(1).setCellValue("상호");
            header.createCell(2).setCellValue("적요");
            header.createCell(3).setCellValue("수입");
            header.createCell(4).setCellValue("비용");
            header.createCell(5).setCellValue("VAT");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-05-03");
            row.createCell(1).setCellValue("카카오광고");
            row.createCell(2).setCellValue("광고비");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue(55000);
            row.createCell(5).setCellValue(5000);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private record AuthFixture(String token, String businessId) {
    }
}
