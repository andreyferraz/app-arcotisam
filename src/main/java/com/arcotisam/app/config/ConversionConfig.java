package com.arcotisam.app.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
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

    @Bean
    public JdbcCustomConversions jdbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new IntegerToBooleanConverter());
        converters.add(new BooleanToIntegerConverter());
        return new JdbcCustomConversions(converters);
    }

}
