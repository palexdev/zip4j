package io.github.palexdev.zip4j.model.enums;

import io.github.palexdev.zip4j.exception.ZipException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class CompressionMethodTest {

	@Test
	public void testGetCompressionMethodFromCodeForUnknownTypeThrowsException() {
		try {
			CompressionMethod.getCompressionMethodFromCode(34);
			fail("Should throw an exception");
		} catch (ZipException e) {
			assertThat(e.getType()).isEqualTo(ZipException.Type.UNKNOWN_COMPRESSION_METHOD);
		}
	}

	@Test
	public void testGetCompressionMethodFromCodeForDeflateReturnsDeflate() throws ZipException {
		assertThat(CompressionMethod.getCompressionMethodFromCode(8)).isEqualTo(CompressionMethod.DEFLATE);
	}

}