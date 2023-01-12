package io.github.palexdev.zip4j.util;

import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.AESExtraDataRecord;
import io.github.palexdev.zip4j.model.LocalFileHeader;
import io.github.palexdev.zip4j.model.enums.CompressionMethod;
import io.github.palexdev.zip4j.testutils.ControlledReadInputStream;
import io.github.palexdev.zip4j.testutils.RandomInputStream;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Zip4jUtilTest {

	@Test
	public void testIsStringNotNullAndNotEmptyReturnsFalseWhenNull() {
		assertThat(Zip4jUtil.isStringNotNullAndNotEmpty(null)).isFalse();
	}

	@Test
	public void testIsStringNotNullAndNotEmptyReturnsFalseWhenEmpty() {
		assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("")).isFalse();
	}

	@Test
	public void testIsStringNotNullAndNotEmptyReturnsFalseWithWhitespaces() {
		assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("   ")).isFalse();
	}

	@Test
	public void testIsStringNotNullAndNotEmptyReturnsTrueForValidString() {
		assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("  Some string   ")).isTrue();
	}

	@Test
	public void testCreateDirectoryIfNotExistsThrowsExceptionWhenPathIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> Zip4jUtil.createDirectoryIfNotExists(null),
				"output path is null"
		);
	}

	@Test
	public void testCreateDirectoryIfNotExistsThrowsExceptionWhenFileExistsButNotDirectory() throws ZipException {
		File file = mock(File.class);
		when(file.exists()).thenReturn(true);
		when(file.isDirectory()).thenReturn(false);

		assertThrows(
				ZipException.class,
				() -> Zip4jUtil.createDirectoryIfNotExists(file),
				"output directory is not valid"
		);
	}

	@Test
	public void testCreateDirectoryIfNotExistsReturnsTrueWhenFileExistsAndIsDirectory() throws ZipException {
		File file = mock(File.class);
		when(file.exists()).thenReturn(true);
		when(file.isDirectory()).thenReturn(true);

		assertThat(Zip4jUtil.createDirectoryIfNotExists(file)).isTrue();
	}

	@Test
	public void testCreateDirectoryIfNotExistsThrowsExceptionWhenFileDoesNotExistAndCannotCreate() throws ZipException {
		File file = mock(File.class);
		when(file.exists()).thenReturn(false);
		when(file.mkdirs()).thenReturn(false);

		assertThrows(
				ZipException.class,
				() -> Zip4jUtil.createDirectoryIfNotExists(file),
				"Cannot create output directories"
		);
	}

	@Test
	public void testCreateDirectoryIfNotExistsReturnsTrueWhenFileDoesNotExistAndCreated() throws ZipException {
		File file = mock(File.class);
		when(file.exists()).thenReturn(false);
		when(file.mkdirs()).thenReturn(true);

		assertThat(Zip4jUtil.createDirectoryIfNotExists(file)).isTrue();
	}

	@Test
	public void testJavaToDosTime() {
		TimeZone defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
		assertThat(Zip4jUtil.epochToExtendedDosTime(1560526564000L)).isEqualTo(1322159234);
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void testDosToJavaTime() {
		TimeZone defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
		assertThat(Zip4jUtil.dosToExtendedEpochTme(1322159234)).isEqualTo(1560526564000L);
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void testConvertCharArrayToByteArrayWithoutUtf8() {
		char[] charArray = "CharArray".toCharArray();

		byte[] byteArray = Zip4jUtil.convertCharArrayToByteArray(charArray, false);

		assertThat(byteArray.length).isEqualTo(charArray.length);
		assertThat(byteArray[0]).isEqualTo((byte) 'C');
		assertThat(byteArray[1]).isEqualTo((byte) 'h');
		assertThat(byteArray[2]).isEqualTo((byte) 'a');
		assertThat(byteArray[3]).isEqualTo((byte) 'r');
		assertThat(byteArray[4]).isEqualTo((byte) 'A');
		assertThat(byteArray[5]).isEqualTo((byte) 'r');
		assertThat(byteArray[6]).isEqualTo((byte) 'r');
		assertThat(byteArray[7]).isEqualTo((byte) 'a');
		assertThat(byteArray[8]).isEqualTo((byte) 'y');
	}

	@Test
	public void testConvertCharArrayToByteArrayChineseChars() {
		char[] charArray = "你好".toCharArray();

		byte[] byteArray = Zip4jUtil.convertCharArrayToByteArray(charArray, true);

		try {
			// Make sure that StandardCharsets exists on the classpath
			Class.forName("java.nio.charset.StandardCharsets");
			assertThat(byteArray.length).isEqualTo(6);
			assertThat(byteArray).isEqualTo(new byte[]{-28, -67, -96, -27, -91, -67});
		} catch (ClassNotFoundException e) {
			// In some test environments (old Android SDK), StandardCharset class does not exist, in this case
			// the method under test falls back to converting char to its byte representation
			assertThat(byteArray.length).isEqualTo(2);
			assertThat(byteArray).isEqualTo(new byte[]{96, 125});
		}
	}

	@Test
	public void testGetCompressionMethodForNonAesReturnsAsIs() throws ZipException {
		LocalFileHeader localFileHeader = new LocalFileHeader();
		localFileHeader.setCompressionMethod(CompressionMethod.DEFLATE);

		assertThat(Zip4jUtil.getCompressionMethod(localFileHeader)).isEqualTo(CompressionMethod.DEFLATE);
	}

	@Test
	public void testGetCompressionMethodForAesWhenAesExtraDataMissingThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> {
					LocalFileHeader localFileHeader = new LocalFileHeader();
					localFileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);
					Zip4jUtil.getCompressionMethod(localFileHeader);
				},
				"AesExtraDataRecord not present in local header for aes encrypted data"
		);
	}

	@Test
	public void testGetCompressionMethidForAesReturnsFromAesExtraDataRecord() throws ZipException {
		AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();
		aesExtraDataRecord.setCompressionMethod(CompressionMethod.STORE);

		LocalFileHeader localFileHeader = new LocalFileHeader();
		localFileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);
		localFileHeader.setAesExtraDataRecord(aesExtraDataRecord);

		assertThat(Zip4jUtil.getCompressionMethod(localFileHeader)).isEqualTo(CompressionMethod.STORE);
	}

	@Test
	public void testReadFullyReadsCompleteBuffer() throws IOException {
		byte[] b = new byte[3423];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(1000);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b)).isEqualTo(3423);
	}

	@Test
	public void testReadFullyReadsCompleteBufferInOneShot() throws IOException {
		byte[] b = new byte[4096];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(4097);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b)).isEqualTo(4096);
	}

	@Test
	public void testReadFullyThrowsExceptionWhenCannotFillBuffer() throws IOException {
		byte[] b = new byte[4097];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(500);

		assertThrows(
				IOException.class,
				() -> Zip4jUtil.readFully(controlledReadInputStream, b),
				"Cannot read fully into byte buffer"
		);
	}

	@Test
	public void testReadFullyOnEmptyStreamThrowsException() throws IOException {
		byte[] b = new byte[4096];
		RandomInputStream randomInputStream = new RandomInputStream(0);
		ControlledReadInputStream controlledReadInputStream = new ControlledReadInputStream(randomInputStream, 100);

		assertThrows(
				IOException.class,
				() -> assertThat(Zip4jUtil.readFully(controlledReadInputStream, b)).isEqualTo(-1),
				"Unexpected EOF reached when trying to read stream"
		);
	}

	@Test
	public void testReadFullyThrowsExceptionWhenRetryLimitExceeds() throws IOException {
		byte[] b = new byte[151];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(10);

		assertThrows(
				IOException.class,
				() -> Zip4jUtil.readFully(controlledReadInputStream, b),
				"Cannot read fully into byte buffer"
		);
	}

	@Test
	public void testReadFullyWithLengthReadsCompleteLength() throws IOException {
		byte[] b = new byte[1000];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(100);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b, 0, 900)).isEqualTo(900);
	}

	@Test
	public void testReadFullyWithLengthReadsMaximumAvailable() throws IOException {
		byte[] b = new byte[1000];
		RandomInputStream randomInputStream = new RandomInputStream(150);
		ControlledReadInputStream controlledReadInputStream = new ControlledReadInputStream(randomInputStream, 700);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b, 0, 900)).isEqualTo(150);
	}

	@Test
	public void testReadFullyWithLengthReadsCompletelyIntoBuffer() throws IOException {
		byte[] b = new byte[1000];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(10);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b, 0, 1000)).isEqualTo(1000);
	}

	@Test
	public void testReadFullyWithNegativeLengthThrowsException() throws IOException {
		byte[] b = new byte[1000];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(10);

		assertThrows(
				IllegalArgumentException.class,
				() -> Zip4jUtil.readFully(controlledReadInputStream, b, 0, -5),
				"Negative length"
		);
	}

	@Test
	public void testReadFullyWithNegativeOffsetThrowsException() throws IOException {
		byte[] b = new byte[10];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(10);

		assertThrows(
				IllegalArgumentException.class,
				() -> Zip4jUtil.readFully(controlledReadInputStream, b, -4, 10),
				"Negative offset"
		);
	}

	@Test
	public void testReadFullyWithLengthZeroReturnsZero() throws IOException {
		byte[] b = new byte[1000];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(100);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b, 0, 0)).isZero();
	}

	@Test
	public void testReadFullyThrowsExceptionWhenOffsetPlusLengthGreaterThanBufferSize() throws IOException {
		byte[] b = new byte[10];
		ControlledReadInputStream controlledReadInputStream = initialiseControlledInputStream(10);

		assertThrows(
				IllegalArgumentException.class,
				() -> Zip4jUtil.readFully(controlledReadInputStream, b, 5, 10),
				"Length greater than buffer size"
		);
	}

	@Test
	public void testReadFullyWithLengthOnAnEmptyStreamReturnsEOF() throws IOException {
		byte[] b = new byte[1000];
		RandomInputStream randomInputStream = new RandomInputStream(-1);
		ControlledReadInputStream controlledReadInputStream = new ControlledReadInputStream(randomInputStream, 100);

		assertThat(Zip4jUtil.readFully(controlledReadInputStream, b, 0, 100)).isEqualTo(-1);
	}

	private ControlledReadInputStream initialiseControlledInputStream(int maxLengthToReadAtOnce) {
		RandomInputStream randomInputStream = new RandomInputStream(4096);
		return new ControlledReadInputStream(randomInputStream, maxLengthToReadAtOnce);
	}
}
