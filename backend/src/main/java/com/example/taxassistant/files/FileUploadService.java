package com.example.taxassistant.files;

import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BusinessVerificationStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.file.UploadedFile;
import com.example.taxassistant.domain.file.UploadedFileParseError;
import com.example.taxassistant.domain.file.UploadedFileParseErrorRepository;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.files.dto.FileParseErrorResponse;
import com.example.taxassistant.files.dto.FileUploadResponse;
import com.example.taxassistant.files.dto.UploadedFileResponse;
import com.example.taxassistant.files.parser.ParseFailure;
import com.example.taxassistant.files.parser.ParsedTransactionRow;
import com.example.taxassistant.files.parser.TransactionFileParser;
import com.example.taxassistant.files.parser.TransactionParseResult;
import com.example.taxassistant.security.ResourceOwnershipService;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("csv", "xlsx");

    private final UploadedFileRepository uploadedFileRepository;
    private final UploadedFileParseErrorRepository parseErrorRepository;
    private final TransactionRepository transactionRepository;
    private final ResourceOwnershipService ownershipService;
    private final List<TransactionFileParser> parsers;
    private final Path storageRoot;
    private final long maxSizeBytes;

    public FileUploadService(
            UploadedFileRepository uploadedFileRepository,
            UploadedFileParseErrorRepository parseErrorRepository,
            TransactionRepository transactionRepository,
            ResourceOwnershipService ownershipService,
            List<TransactionFileParser> parsers,
            @Value("${app.files.local-storage-path}") String storageRoot,
            @Value("${app.files.max-size-bytes}") long maxSizeBytes
    ) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.parseErrorRepository = parseErrorRepository;
        this.transactionRepository = transactionRepository;
        this.ownershipService = ownershipService;
        this.parsers = parsers;
        this.storageRoot = Path.of(storageRoot);
        this.maxSizeBytes = maxSizeBytes;
    }

    @Transactional
    public FileUploadResponse upload(UUID userId, UUID businessId, MultipartFile multipartFile) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        if (business.getVerificationStatus() != BusinessVerificationStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.BUSINESS_VERIFICATION_REQUIRED);
        }
        validateFile(multipartFile);

        String originalFilename = safeFilename(multipartFile.getOriginalFilename());
        String extension = extension(originalFilename);
        byte[] content = readBytes(multipartFile);
        String storageKey = storeFile(businessId, extension, content);

        UploadedFile uploadedFile = new UploadedFile(
                business,
                originalFilename,
                storageKey,
                multipartFile.getContentType(),
                multipartFile.getSize()
        );
        uploadedFile.updateChecksum(sha256(content));
        uploadedFile.markParsing();
        uploadedFileRepository.save(uploadedFile);

        TransactionParseResult parseResult = parserFor(extension).parse(content);
        List<Transaction> transactions = parseResult.rows()
                .stream()
                .map(row -> toTransaction(business, uploadedFile, row))
                .toList();
        transactionRepository.saveAll(transactions);

        List<UploadedFileParseError> errors = parseResult.failures()
                .stream()
                .map(failure -> toParseError(uploadedFile, failure))
                .toList();
        parseErrorRepository.saveAll(errors);

        if (transactions.isEmpty() && !errors.isEmpty()) {
            uploadedFile.markFailed("파싱 가능한 거래 행이 없습니다.");
        } else {
            uploadedFile.markParsed();
        }

        return new FileUploadResponse(
                uploadedFile.getId(),
                uploadedFile.getOriginalFilename(),
                uploadedFile.getProcessingStatus(),
                transactions.size(),
                errors.size(),
                errors.stream()
                        .limit(50)
                        .map(error -> new FileParseErrorResponse(
                                error.getRowNumber(),
                                error.getMessage(),
                                error.getRawData()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<UploadedFileResponse> findAll(UUID userId, UUID businessId) {
        ownershipService.requireOwnedBusiness(userId, businessId);
        return uploadedFileRepository.findAllByBusinessIdAndBusinessOwnerIdOrderByCreatedAtDesc(businessId, userId)
                .stream()
                .map(UploadedFileResponse::from)
                .toList();
    }

    private void validateFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (multipartFile.getSize() > maxSizeBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String originalFilename = safeFilename(multipartFile.getOriginalFilename());
        String extension = extension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private TransactionFileParser parserFor(String extension) {
        return parsers.stream()
                .filter(parser -> parser.supports(extension))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE));
    }

    private Transaction toTransaction(Business business, UploadedFile uploadedFile, ParsedTransactionRow row) {
        Transaction transaction = new Transaction(
                business,
                uploadedFile,
                row.transactionDate(),
                row.merchantName(),
                row.description(),
                row.amount(),
                row.income() ? TransactionType.INCOME : TransactionType.EXPENSE
        );
        transaction.updateVatAmount(row.vatAmount());
        transaction.attachRawData(row.rowNumber(), row.rawData());
        return transaction;
    }

    private UploadedFileParseError toParseError(UploadedFile uploadedFile, ParseFailure failure) {
        return new UploadedFileParseError(
                uploadedFile,
                failure.rowNumber(),
                failure.message(),
                failure.rawData()
        );
    }

    private String storeFile(UUID businessId, String extension, byte[] content) {
        try {
            Path businessDirectory = storageRoot.resolve(businessId.toString());
            Files.createDirectories(businessDirectory);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = businessDirectory.resolve(filename);
            Files.write(target, content);
            return storageRoot.relativize(target).toString().replace("\\", "/");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private byte[] readBytes(MultipartFile multipartFile) {
        try {
            return multipartFile.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return String.format("%064x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private String extension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
