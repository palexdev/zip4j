package io.github.palexdev.zip4j.io.inputstream;

import io.github.palexdev.zip4j.AbstractIT;
import io.github.palexdev.zip4j.model.enums.RandomAccessFileMode;
import io.github.palexdev.zip4j.testutils.TestUtils;
import io.github.palexdev.zip4j.util.CrcUtil;
import io.github.palexdev.zip4j.util.InternalZipConstants;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.zip.CRC32;

import static io.github.palexdev.zip4j.testutils.TestUtils.getTestFileFromResources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NumberedSplitRandomAccessFileIT extends AbstractIT {

	private static final int ONE_HUNDRED_KB = 102400;

	@Test
	public void testOpenInWriteModeThrowsException() throws IOException {
		assertThrows(
				IllegalArgumentException.class,
				() -> new NumberedSplitRandomAccessFile(getTestFileFromResources("sample.pdf"), RandomAccessFileMode.WRITE.getValue()),
				"write mode is not allowed for NumberedSplitRandomAccessFile"
		);

	}

	@Test
	public void testWriteIntThrowsUnsupportedOperationException() throws IOException {
		assertThrows(
				UnsupportedOperationException.class,
				() -> {
					String fileName = "somefile.001";
					File splitFile = Files.createFile(temporaryFolder.resolve(fileName)).toFile();
					NumberedSplitRandomAccessFile numberedSplitRandomAccessFile = new NumberedSplitRandomAccessFile(
							splitFile, RandomAccessFileMode.READ.getValue());
					numberedSplitRandomAccessFile.write(1);
				}
		);
	}

	@Test
	public void testWriteByteArrayThrowsUnsupportedOperationException() throws IOException {
		assertThrows(
				UnsupportedOperationException.class,
				() -> {
					String fileName = "somefile.001";
					File splitFile = Files.createFile(temporaryFolder.resolve(fileName)).toFile();
					NumberedSplitRandomAccessFile numberedSplitRandomAccessFile = new NumberedSplitRandomAccessFile(
							splitFile, RandomAccessFileMode.READ.getValue());
					numberedSplitRandomAccessFile.write(new byte[100]);
				}
		);
	}

	@Test
	public void testWriteByteArrayWithLengthThrowsUnsupportedOperationException() throws IOException {
		assertThrows(
				UnsupportedOperationException.class,
				() -> {
					String fileName = "somefile.001";
					File splitFile = Files.createFile(temporaryFolder.resolve(fileName)).toFile();
					NumberedSplitRandomAccessFile numberedSplitRandomAccessFile = new NumberedSplitRandomAccessFile(
							splitFile, RandomAccessFileMode.READ.getValue());
					numberedSplitRandomAccessFile.write(new byte[100], 0, 10);
				}
		);
	}

	@Test
	public void testReadIntReadsCompleteFileSuccessfully() throws IOException {
		File fileToSplit = getTestFileFromResources("file_PDF_1MB.pdf");
		long sourceFileCrc = CrcUtil.computeFileCrc(fileToSplit, null);
		File firstSplitFile = TestUtils.splitFileWith7ZipFormat(fileToSplit, temporaryFolder.toFile(), InternalZipConstants.MIN_SPLIT_LENGTH);

		CRC32 crc32 = new CRC32();
		int readVal;

		try (NumberedSplitRandomAccessFile randomAccessFile = openSplitFile(firstSplitFile)) {
			while ((readVal = randomAccessFile.read()) != -1) {
				crc32.update(readVal);
			}
		}

		assertThat(crc32.getValue()).isEqualTo(sourceFileCrc);
	}

	@Test
	public void testReadByteArrayReadsCompleteFileSuccessfully() throws IOException {
		File fileToSplit = getTestFileFromResources("file_PDF_1MB.pdf");
		long sourceFileCrc = CrcUtil.computeFileCrc(fileToSplit, null);
		File firstSplitFile = TestUtils.splitFileWith7ZipFormat(fileToSplit, temporaryFolder.toFile(), ONE_HUNDRED_KB);

		CRC32 crc32 = new CRC32();
		byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
		int readLen;

		try (NumberedSplitRandomAccessFile randomAccessFile = openSplitFile(firstSplitFile)) {
			while ((readLen = randomAccessFile.read(buff)) != -1) {
				crc32.update(buff, 0, readLen);
			}
		}

		assertThat(crc32.getValue()).isEqualTo(sourceFileCrc);
	}

	@Test
	public void testReadByteArrayWithLengthReadsCompleteFileSuccessfully() throws IOException {
		File fileToSplit = getTestFileFromResources("file_PDF_1MB.pdf");
		long sourceFileCrc = CrcUtil.computeFileCrc(fileToSplit, null);
		File firstSplitFile = TestUtils.splitFileWith7ZipFormat(fileToSplit, temporaryFolder.toFile(), ONE_HUNDRED_KB - 100);

		CRC32 crc32 = new CRC32();
		byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
		int readLen;

		try (NumberedSplitRandomAccessFile randomAccessFile = openSplitFile(firstSplitFile)) {
			while ((readLen = randomAccessFile.read(buff, 0, 100)) != -1) {
				crc32.update(buff, 0, readLen);
			}
		}

		assertThat(crc32.getValue()).isEqualTo(sourceFileCrc);
	}

	@Test
	public void testSeek() throws IOException {
		File fileToSplit = getTestFileFromResources("file_PDF_1MB.pdf");
		File firstSplitFile = TestUtils.splitFileWith7ZipFormat(fileToSplit, temporaryFolder.toFile(), ONE_HUNDRED_KB);

		try (NumberedSplitRandomAccessFile splitRandomAccessFile = openSplitFile(firstSplitFile);
		     RandomAccessFile sourceRandomAccessFile = new RandomAccessFile(fileToSplit, RandomAccessFileMode.READ.getValue())) {

			splitRandomAccessFile.seek(fileToSplit.length() - 200);
			sourceRandomAccessFile.seek(fileToSplit.length() - 200);

			byte[] sourceBytes = new byte[200];
			byte[] splitFileBytes = new byte[200];

			sourceRandomAccessFile.read(sourceBytes);
			splitRandomAccessFile.read(splitFileBytes);

			assertThat(splitFileBytes).isEqualTo(sourceBytes);
		}
	}

	private NumberedSplitRandomAccessFile openSplitFile(File firstSplitFile) throws IOException {
		return new NumberedSplitRandomAccessFile(firstSplitFile, RandomAccessFileMode.READ.getValue());
	}
}