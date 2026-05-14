package com.arcotisam.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.relational.core.dialect.AnsiDialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.render.SelectRenderContext;

@Configuration
public class DbConfig {
	@Bean
	public JdbcDialect jdbcDialect() {
		return new JdbcDialect() {
			@Override
			public LimitClause limit() {
				return AnsiDialect.INSTANCE.limit();
			}

			@Override
			public LockClause lock() {
				return AnsiDialect.INSTANCE.lock();
			}

			@Override
			public SelectRenderContext getSelectContext() {
				return AnsiDialect.INSTANCE.getSelectContext();
			}
		};
	}

}
