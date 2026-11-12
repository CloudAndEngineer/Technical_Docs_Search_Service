package org.example.ingestion.repository;

public record IndexedChunk(String content, float[] embedding) {
}
