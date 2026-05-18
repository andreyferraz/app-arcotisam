package com.arcotisam.app.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import com.arcotisam.app.enuns.Role;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;

@Configuration
public class ConversionConfig {

    static class IntegerToBooleanConverter implements Converter<Integer, Boolean> {
        @Override
        public Boolean convert(Integer source) {
            if (source == null) return null;
            return source != 0;
        }
    }

    static class BooleanToIntegerConverter implements Converter<Boolean, Integer> {
        @Override
        public Integer convert(Boolean source) {
            if (source == null) return null;
            return source ? 1 : 0;
        }
    }

    static class StringToOffsetDateTimeConverter implements Converter<String, java.time.OffsetDateTime> {
        @Override
        public java.time.OffsetDateTime convert(String source) {
            if (source == null) return null;
            try {
                return java.time.OffsetDateTime.parse(source);
            } catch (java.time.format.DateTimeParseException ex) {
                // try common fallbacks: epoch millis, or local date/time
                try {
                    long epoch = Long.parseLong(source);
                    return java.time.Instant.ofEpochMilli(epoch).atOffset(java.time.ZoneOffset.UTC);
                } catch (Exception e) {
                    try {
                        java.time.LocalDate ld = java.time.LocalDate.parse(source);
                        return ld.atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
                    } catch (Exception e2) {
                        // unable to parse - return null so mapping continues without exception
                        return null;
                    }
                }
            }
        }
    }

    static class OffsetDateTimeToStringConverter implements Converter<java.time.OffsetDateTime, String> {
        @Override
        public String convert(java.time.OffsetDateTime source) {
            if (source == null) return null;
            return source.toString();
        }
    }

    static class StringToRoleConverter implements Converter<String, Role> {
        @Override
        public Role convert(String source) {
            if (source == null) return null;
            try {
                return Role.valueOf(source);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    static class RoleToStringConverter implements Converter<Role, String> {
        @Override
        public String convert(Role source) {
            if (source == null) return null;
            return source.name();
        }
    }

    @Bean
    public JdbcCustomConversions jdbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new IntegerToBooleanConverter());
        converters.add(new BooleanToIntegerConverter());
        converters.add(new StringToOffsetDateTimeConverter());
        converters.add(new OffsetDateTimeToStringConverter());
        converters.add(new StringToRoleConverter());
        converters.add(new RoleToStringConverter());
        return new JdbcCustomConversions(converters);
    }

}
