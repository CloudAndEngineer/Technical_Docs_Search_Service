package org.example.ingestion.service;

import org.example.ingestion.exception.IngestionException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class WebSourceFetcher {

	private final int maxBytes;

	public WebSourceFetcher(@Value("${ingestion.max-web-source-bytes}") int maxBytes) {
		this.maxBytes = maxBytes;
	}

	public FetchedSource fetch(String rawUrl) {
		URI uri = validateAndResolve(rawUrl);
		try {
			for (int redirect = 0; redirect <= 3; redirect++) {
				Connection.Response response = Jsoup.connect(uri.toString())
						.userAgent("KnowledgeWorkspaceBot/1.0")
						.timeout(15000)
						.followRedirects(false)
						.ignoreHttpErrors(true)
						.maxBodySize(maxBytes)
						.execute();
				if (response.statusCode() >= 300 && response.statusCode() < 400) {
					String location = response.header("Location");
					if (redirect == 3 || location == null || location.isBlank()) {
						throw new IngestionException("SOURCE_REDIRECT_REJECTED", "문서 URL의 리디렉션 횟수가 너무 많거나 주소가 올바르지 않습니다.");
					}
					uri = validateAndResolve(uri.resolve(location).toString());
					continue;
				}
				if (response.statusCode() < 200 || response.statusCode() >= 300) {
					throw new IngestionException("SOURCE_FETCH_FAILED", "검색된 문서를 가져오지 못했습니다 (HTTP " + response.statusCode() + ").");
				}
				String contentLength = response.header("Content-Length");
				if (contentLength != null && Long.parseLong(contentLength) > maxBytes) {
					throw new IngestionException("SOURCE_TOO_LARGE", "검색된 문서가 허용 크기를 초과했습니다.");
				}
				byte[] bytes = response.bodyAsBytes();
				if (bytes.length == 0) {
					throw new IngestionException("EMPTY_WEB_DOCUMENT", "검색된 문서의 내용이 비어 있습니다.");
				}
				if (bytes.length >= maxBytes) {
					throw new IngestionException("SOURCE_TOO_LARGE", "검색된 문서가 허용 크기를 초과했거나 완전히 수신되지 않았습니다.");
				}
				Document parsed = Jsoup.parse(new String(bytes, response.charset() == null ? "UTF-8" : response.charset()), uri.toString());
				String title = parsed.title().isBlank() ? uri.getHost() : parsed.title();
				return new FetchedSource(title, uri.toString(), response.contentType(), bytes);
			}
			throw new IngestionException("SOURCE_REDIRECT_REJECTED", "문서 URL을 가져오지 못했습니다.");
		} catch (IngestionException exception) {
			throw exception;
		} catch (NumberFormatException exception) {
			throw new IngestionException("SOURCE_FETCH_FAILED", "검색된 문서의 응답 헤더가 올바르지 않습니다.", exception);
		} catch (Exception exception) {
			throw new IngestionException("SOURCE_FETCH_FAILED", "검색된 문서를 가져오거나 읽지 못했습니다.", exception);
		}
	}

	private URI validateAndResolve(String rawUrl) {
		try {
			URI uri = URI.create(rawUrl).normalize();
			if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
					|| uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
				throw new IngestionException("INVALID_SOURCE_URL", "검색 결과에 유효한 HTTPS 문서 주소가 없습니다.");
			}
			for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
				if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
						|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
					throw new IngestionException("INVALID_SOURCE_URL", "내부 네트워크 주소는 문서로 등록할 수 없습니다.");
				}
			}
			return uri;
		} catch (IngestionException exception) {
			throw exception;
		} catch (UnknownHostException | IllegalArgumentException exception) {
			throw new IngestionException("INVALID_SOURCE_URL", "검색 결과 URL이 유효하지 않습니다.", exception);
		}
	}

	public record FetchedSource(String title, String url, String contentType, byte[] bytes) {
	}
}
