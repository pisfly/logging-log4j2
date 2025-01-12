/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.pattern;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

/**
 * Converts and formats the event's date in a StringBuilder.
 */
@Plugin(name = "DatePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "d", "date" })
public final class DatePatternConverter extends LogEventPatternConverter implements ArrayPatternConverter {

    private final class CurrentTime {
        public long timestampMillis;
        public String formatted;

        public CurrentTime(final long timestampMillis) {
            this.timestampMillis = timestampMillis;
            this.formatted = formatter.format(this.timestampMillis);
        }
    }

    private final AtomicReference<CurrentTime> currentTime;

    private abstract static class Formatter {
        abstract String format(long timeMillis);

        public String toPattern() {
            return null;
        }
    }

    private static final class PatternFormatter extends Formatter {
        private final FastDateFormat fastDateFormat;

        PatternFormatter(final FastDateFormat fastDateFormat) {
            this.fastDateFormat = fastDateFormat;
        }

        @Override
        String format(final long timeMillis) {
            return fastDateFormat.format(timeMillis);
        }

        @Override
        public String toPattern() {
            return fastDateFormat.toPattern();
        }
    }

    private static final class UnixFormatter extends Formatter {

        @Override
        String format(final long timeMillis) {
            return Long.toString(timeMillis / 1000);
        }

    }

    private static final class UnixMillisFormatter extends Formatter {

        @Override
        String format(final long timeMillis) {
            return Long.toString(timeMillis);
        }

    }

    /**
     * ABSOLUTE string literal.
     */
    private static final String ABSOLUTE_FORMAT = "ABSOLUTE";

    /**
     * SimpleTimePattern for ABSOLUTE.
     */
    private static final String ABSOLUTE_TIME_PATTERN = "HH:mm:ss,SSS";

    /**
     * COMPACT string literal.
     */
    private static final String COMPACT_FORMAT = "COMPACT";

    /**
     * SimpleTimePattern for COMPACT.
     */
    private static final String COMPACT_PATTERN = "yyyyMMddHHmmssSSS";

    /**
     * DATE string literal.
     */
    private static final String DATE_AND_TIME_FORMAT = "DATE";

    /**
     * SimpleTimePattern for DATE.
     */
    private static final String DATE_AND_TIME_PATTERN = "dd MMM yyyy HH:mm:ss,SSS";

    /**
     * DEFAULT string literal.
     */
    private static final String DEFAULT_FORMAT = "DEFAULT";

    /**
     * SimpleTimePattern for DEFAULT.
     */
    // package private for unit tests
    static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    /**
     * ISO8601_BASIC string literal.
     */
    private static final String ISO8601_BASIC_FORMAT = "ISO8601_BASIC";

    /**
     * SimpleTimePattern for ISO8601_BASIC.
     */
    private static final String ISO8601_BASIC_PATTERN = "yyyyMMdd'T'HHmmss,SSS";

    /**
     * ISO8601 string literal.
     */
    // package private for unit tests
    static final String ISO8601_FORMAT = "ISO8601";

    /**
     * SimpleTimePattern for ISO8601.
     */
    // package private for unit tests
    static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss,SSS";

    /**
     * UNIX formatter in seconds (standard).
     */
    private static final String UNIX_FORMAT = "UNIX";

    /**
     * UNIX formatter in milliseconds
     */
    private static final String UNIX_MILLIS_FORMAT = "UNIX_MILLIS";

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options
     *            options, may be null.
     * @return instance of pattern converter.
     */
    public static DatePatternConverter newInstance(final String[] options) {
        return new DatePatternConverter(options);
    }

    private final Formatter formatter;

    /**
     * Private constructor.
     *
     * @param options
     *            options, may be null.
     */
    private DatePatternConverter(final String[] options) {
        super("Date", "date");

        // null patternOption is OK.
        final String patternOption = options != null && options.length > 0 ? options[0] : null;

        String pattern = null;
        Formatter tempFormatter = null;

        if (patternOption == null || patternOption.equalsIgnoreCase(DEFAULT_FORMAT)) {
            pattern = DEFAULT_PATTERN;
        } else if (patternOption.equalsIgnoreCase(ISO8601_FORMAT)) {
            pattern = ISO8601_PATTERN;
        } else if (patternOption.equalsIgnoreCase(ISO8601_BASIC_FORMAT)) {
            pattern = ISO8601_BASIC_PATTERN;
        } else if (patternOption.equalsIgnoreCase(ABSOLUTE_FORMAT)) {
            pattern = ABSOLUTE_TIME_PATTERN;
        } else if (patternOption.equalsIgnoreCase(DATE_AND_TIME_FORMAT)) {
            pattern = DATE_AND_TIME_PATTERN;
        } else if (patternOption.equalsIgnoreCase(COMPACT_FORMAT)) {
            pattern = COMPACT_PATTERN;
        } else if (patternOption.equalsIgnoreCase(UNIX_FORMAT)) {
            tempFormatter = new UnixFormatter();
        } else if (patternOption.equalsIgnoreCase(UNIX_MILLIS_FORMAT)) {
            tempFormatter = new UnixMillisFormatter();
        } else {
            pattern = patternOption;
        }

        if (pattern != null) {
            FastDateFormat tempFormat;

            TimeZone tz = null;

            // if the option list contains a TZ option, then set it.
            if (options != null && options.length > 1) {
                tz = TimeZone.getTimeZone(options[1]);
            }

            try {
                tempFormat = FastDateFormat.getInstance(pattern, tz);
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Could not instantiate FastDateFormat with pattern " + patternOption, e);

                // default to the DEFAULT format
                tempFormat = FastDateFormat.getInstance(DEFAULT_PATTERN);
            }


            tempFormatter = new PatternFormatter(tempFormat);
        }
        formatter = tempFormatter;
        currentTime = new AtomicReference<>(new CurrentTime(System.currentTimeMillis()));
    }

    /**
     * Appends formatted date to string buffer.
     *
     * @param date
     *            date
     * @param toAppendTo
     *            buffer to which formatted date is appended.
     */
    public void format(final Date date, final StringBuilder toAppendTo) {
            toAppendTo.append(formatter.format(date.getTime()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        final long timestampMillis = event.getTimeMillis();
        CurrentTime current = currentTime.get();
        if (timestampMillis != current.timestampMillis) {
            final CurrentTime newTime = new CurrentTime(timestampMillis);
            if (currentTime.compareAndSet(current, newTime)) {
                current = newTime;
            } else {
                current = currentTime.get();
            }
        }

        output.append(current.formatted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object obj, final StringBuilder output) {
        if (obj instanceof Date) {
            format((Date) obj, output);
        }
        super.format(obj, output);
    }

    @Override
    public void format(final StringBuilder toAppendTo, final Object... objects) {
        for (final Object obj : objects) {
            if (obj instanceof Date) {
                format(obj, toAppendTo);
                break;
            }
        }
    }

    /**
     * Gets the pattern string describing this date format.
     *
     * @return the pattern string describing this date format.
     */
    public String getPattern() {
        return formatter.toPattern();
    }

}
