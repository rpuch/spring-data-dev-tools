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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.jdbc.core.convert.NewJdbcConverter;
import org.springframework.data.jdbc.core.convert.NewJdbcConverterImpl;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * @author Oliver Drotbohm
 */

public class NewJdbcConverterBenchmark extends AbstractMicrobenchmark {

	JdbcMappingContext context;
	NewJdbcConverter converter;

	@Setup
	public void setUp() {

		this.context = new JdbcMappingContext();
		this.converter = new NewJdbcConverterImpl(context, new EntityInstantiators());
	}

	@Benchmark
	public void mapBook(Blackhole sink) throws Exception {

		Set<String> columns = new HashSet<>(Arrays.asList("id", "title", "pages"));

		HashMap<String, Object> values = new HashMap<>();
		values.put("id", 42);
		values.put("title", "Title");
		values.put("pages", 42);

		MockResultSet foo = new MockResultSet("0");
		foo.addColumns(columns);
		foo.addRow(values);
		foo.next();

		sink.consume(converter.read(Book.class, foo));
	}

	public static void main(String[] args) {

		NewJdbcConverterBenchmark benchmark = new NewJdbcConverterBenchmark();
		benchmark.setUp();

		try {

			Blackhole blackhole = new Blackhole(
					"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

			for (int i = 0; i < 100000; i++) {
				benchmark.mapBook(new Blackhole(
						"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
