/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.microbenchmark.jdbc;

import lombok.SneakyThrows;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.jdbc.core.convert.BasicJdbcConverter;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.NewJdbcConverter;
import org.springframework.data.jdbc.core.convert.NewJdbcConverterImpl;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * @author Oliver Drotbohm
 */
public class DataAccessStrategyBenchmark extends AbstractMicrobenchmark {

	private DataAccessStrategy operations;
	private JdbcOperations jdbc;
	private Book reference;
	private RelationalMappingContext context;
	private EntityRowMapper<?> mapper;
	private JdbcConverter converter;
	private NewJdbcConverter newConverter;

	private RelationalPersistentEntity<?> entity;

	@Setup
	public void setUp() {

		JdbcFixture fixture = new JdbcFixture();

		this.operations = fixture.getContext().getBean(DataAccessStrategy.class);
		this.reference = fixture.getBooks().get(0);
		this.context = fixture.getContext().getBean(RelationalMappingContext.class);
		this.jdbc = fixture.getContext().getBean(JdbcOperations.class);

		this.converter = new BasicJdbcConverter(context, operations);
		this.entity = context.getRequiredPersistentEntity(Book.class);
		this.newConverter = new NewJdbcConverterImpl(context, new EntityInstantiators());
		this.mapper = new EntityRowMapper<>(entity, converter, newConverter);
	}

	@Benchmark
	public void findAllByJdbc(Blackhole sink) {
		sink.consume(jdbc.query("SELECT * FROM Book", mapper));
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(operations.findAll(Book.class));
	}

	@Benchmark
	public void findById(Blackhole sink) {
		sink.consume(operations.findById(reference.getId(), Book.class));
	}

	@SneakyThrows
	public static void main(String[] args) {

		DataAccessStrategyBenchmark benchmark = new DataAccessStrategyBenchmark();
		benchmark.setUp();

		Blackhole blackhole = new Blackhole(
				"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

		for (int i = 0; i < 10000; i++) {
			benchmark.findAllByJdbc(blackhole);
		}
	}
}
