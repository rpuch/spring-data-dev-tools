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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

/**
 * @author Oliver Drotbohm
 */
public class JdbcAggregateOperationsBenchmark extends AbstractMicrobenchmark {

	private JdbcAggregateOperations operations;

	@Setup
	public void setUp() {

		JdbcFixture fixture = new JdbcFixture();

		this.operations = fixture.getContext().getBean(JdbcAggregateOperations.class);
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(operations.findAll(Book.class));
	}
}
