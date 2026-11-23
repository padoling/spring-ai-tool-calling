package com.codeit.chat.support;

final class LogContent {
    private static final int MAX_LENGTH = 1000;

    private LogContent() {
    }

    static String preview(String content) {
        if (content == null) {
            return "";
        }
        String singleLine = content.replace("\r", "\\r").replace("\n", "\\n");
        return singleLine.length() <= MAX_LENGTH ? singleLine : singleLine.substring(0, MAX_LENGTH) + "…";
    }
}
