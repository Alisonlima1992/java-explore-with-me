package ru.practicum.util;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    public static final String APP_NAME = "ewm-main-service";

    public static final String DEFAULT_PAGE_FROM = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";

    public static final int MIN_HOURS_BEFORE_EVENT = 2;
}