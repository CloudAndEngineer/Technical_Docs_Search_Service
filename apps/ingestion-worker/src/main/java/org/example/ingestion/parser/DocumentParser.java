package org.example.ingestion.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.ingestion.exception.IngestionException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class DocumentParser {

	public String parseUpload(String fileName, byte[] bytes) {
		String extension = validateUploadName(fileName);
		return switch (extension) {
			case "pdf" -> parsePdf(bytes);
			case "md", "txt" -> parseText(bytes);
			default -> throw new IngestionException("INVALID_FILE_FORMAT", "PDF, MD, TXT 파일만 등록할 수 있습니다.");
		};
	}

	public String validateUploadName(String fileName) {
		String extension = extension(fileName == null ? "" : fileName);
		if (!extension.equals("pdf") && !extension.equals("md") && !extension.equals("txt")) {
			throw new IngestionException("INVALID_FILE_FORMAT", "PDF, MD, TXT 파일만 등록할 수 있습니다.");
		}
		return extension;
	}

	private String parsePdf(byte[] bytes) {
		if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
			throw new IngestionException("INVALID_PDF", "PDF 파일이 손상되었거나 올바른 PDF 형식이 아닙니다.");
		}
		try (var pdf = Loader.loadPDF(bytes)) {
			String text = new PDFTextStripper().getText(pdf);
			if (text == null || text.isBlank()) {
				throw new IngestionException("EMPTY_DOCUMENT", "PDF에서 추출할 텍스트가 없습니다.");
			}
			return text;
		} catch (IOException exception) {
			throw new IngestionException("INVALID_PDF", "PDF 문서를 읽을 수 없습니다.", exception);
		}
	}

	private String parseText(byte[] bytes) {
		try {
			String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
			if (text.isBlank()) {
				throw new IngestionException("EMPTY_DOCUMENT", "문서에서 추출할 텍스트가 없습니다.");
			}
			return text;
		} catch (CharacterCodingException exception) {
			throw new IngestionException("INVALID_TEXT_ENCODING", "MD와 TXT 파일은 UTF-8 인코딩이어야 합니다.", exception);
		}
	}

	private String extension(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}