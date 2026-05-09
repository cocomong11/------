package com.example.taxassistant.files.parser;

public interface TransactionFileParser {

    boolean supports(String extension);

    TransactionParseResult parse(byte[] content);
}

