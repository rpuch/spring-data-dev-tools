/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.microbenchmark.common;

import jmh.mbr.core.ResultsWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Elasticsearch specific {@link ResultsWriter} implementation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.1
 */
public class ElasticResultsWriter implements ResultsWriter {

	private final String uri;

	public ElasticResultsWriter(String uri) {
		this.uri = uri;
	}

	@Override
	public void write(OutputFormat output, Collection<RunResult> results) {

		List<IndexRequest> computedResults = results.stream() //
				.map(ElasticResultsWriter::asMap) //
				.map(ElasticResultsWriter::createIndexRequest) //
				.collect(Collectors.toList());

		RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(extractHttpHost(uri)));

		computedResults.forEach(it -> {
			try {
				client.index(it, RequestOptions.DEFAULT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static IndexRequest createIndexRequest(Map<String, Object> source) {

		String index = "jmh_" + source.get("project_name");
		IndexRequest request = new IndexRequest(index, "doc");
		request.source(source);
		return request;
	}

	HttpHost extractHttpHost(String uri) {

		String parsableUri = uri;
		if (uri.startsWith("elastic")) {
			parsableUri = uri.replaceFirst("^elastic", "http");
		}

		try {

			URL url = new URL(parsableUri);
			return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

		} catch (MalformedURLException e) {
			return new HttpHost("localhost", 9200);
		}
	}

	/**
	 * Convert a single {@link RunResult} to a generic JMH representation of the JMH Json format and enhance it with
	 * additional {@link Metadata meta information}.
	 *
	 * @param result
	 * @return json string representation of results.
	 * @see org.openjdk.jmh.results.format.JSONResultFormat
	 */
	@SneakyThrows
	static Map<String, Object> asMap(RunResult result) {

		Metadata metadata = new Metadata(new Date(), result);

		Map<String, Object> mapped = new LinkedHashMap<>(
				new ObjectMapper().readValue(ResultsWriter.jsonifyResults(Collections.singleton(result)), Map[].class)[0]);

		mapped.putAll(metadata.asMap());
		return mapped;
	}

	/**
	 * Meta information read from the actual {@link RunResult} and {@link org.springframework.core.env.Environment}. The
	 * data computed here helps creating time series data based on analytic system friendly meta information such as the
	 * project name and version, git commit information and so on.
	 *
	 * @since 2.1
	 * @author Christoph Strobl
	 */
	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	static class Metadata {

		Date date;
		String projectName;
		String projectVersion;
		String gitCommitId;
		String benchmarkGroup;
		String benchmarkName;
		String os;

		Metadata(Date date, RunResult runResult) {

			this.date = date;

			Environment env = new StandardEnvironment();
			this.projectName = env.getProperty("project.name", "unknown");
			this.projectVersion = env.getProperty("project.version", "unknown");
			this.gitCommitId = env.getProperty("git.commit.id", "unknown");
			this.os = env.getProperty("os.name", "unknown");
			this.benchmarkGroup = extractBenchmarkGroup(runResult);
			this.benchmarkName = extractBenchmarkName(runResult);
		}

		public Map<String, Object> asMap() {

			Map<String, Object> metadata = new LinkedHashMap<>();

			metadata.put("date", date);
			metadata.put("project_name", projectName);
			metadata.put("project_version", projectVersion);
			metadata.put("snapshot", projectVersion.toLowerCase().contains("snapshot"));
			metadata.put("git_commit", gitCommitId);
			metadata.put("benchmark_group", benchmarkGroup);
			metadata.put("benchmark_name", benchmarkName);
			metadata.put("operating_system", os);

			return metadata;
		}

		private static String extractBenchmarkName(RunResult result) {

			String source = result.getParams().getBenchmark();
			return source.substring(source.lastIndexOf(".") + 1);
		}

		private static String extractBenchmarkGroup(RunResult result) {

			String source = result.getParams().getBenchmark();
			String tmp = source.substring(0, source.lastIndexOf('.'));
			return tmp.substring(tmp.lastIndexOf(".") + 1);
		}
	}
}
