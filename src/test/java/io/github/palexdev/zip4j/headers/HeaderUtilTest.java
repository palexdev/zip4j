package io.github.palexdev.zip4j.headers;

import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.*;
import io.github.palexdev.zip4j.util.InternalZipConstants;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HeaderUtilTest {

	private static final String FILE_NAME = "test.txt";

	@Test
	public void testGetFileHeaderWithNullZipModelThrowsException() {
		assertThrows(
				ZipException.class,
				() -> HeaderUtil.getFileHeader(null, FILE_NAME),
				"zip model is null, cannot determine file header with exact match for fileName: " + FILE_NAME
		);
	}

	@Test
	public void testGetFileHeaderWithNullFileNameThrowsException() {
		assertThrows(
				ZipException.class,
				() -> HeaderUtil.getFileHeader(new ZipModel(), null),
				"file name is null, cannot determine file header with exact match for fileName: null"
		);
	}

	@Test
	public void testGetFileHeaderWithEmptyFileNameThrowsException() {
		assertThrows(
				ZipException.class,
				() -> HeaderUtil.getFileHeader(new ZipModel(), ""),
				"file name is null, cannot determine file header with exact match for fileName: "
		);
	}

	@Test
	public void testGetFileHeaderWithNullCentralDirectoryThrowsException() {
		assertThrows(
				ZipException.class,
				() -> {
					ZipModel zipModel = new ZipModel();
					zipModel.setCentralDirectory(null);
					HeaderUtil.getFileHeader(zipModel, FILE_NAME);
				},
				"central directory is null, cannot determine file header with exact match for fileName: "
						+ FILE_NAME
		);
	}

	@Test
	public void testGetFileHeaderWithNullFileHeadersThrowsException() {
		assertThrows(
				ZipException.class,
				() -> {
					ZipModel zipModel = new ZipModel();
					CentralDirectory centralDirectory = new CentralDirectory();
					centralDirectory.setFileHeaders(null);
					zipModel.setCentralDirectory(centralDirectory);
					HeaderUtil.getFileHeader(zipModel, FILE_NAME);
				},
				"file Headers are null, cannot determine file header with exact match for fileName: "
						+ FILE_NAME
		);
	}

	@Test
	public void testGetFileHeaderWithEmptyFileHeadersReturnsNull() throws ZipException {
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Collections.emptyList());
		zipModel.setCentralDirectory(centralDirectory);

		FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, FILE_NAME);
		assertThat(fileHeader).isNull();
	}

	@Test
	public void testGetFileHeaderWithExactMatch() throws ZipException {
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Arrays.asList(
				generateFileHeader(null),
				generateFileHeader(""),
				generateFileHeader("SOME_OTHER_NAME"),
				generateFileHeader(FILE_NAME)
		));
		zipModel.setCentralDirectory(centralDirectory);

		FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, FILE_NAME);
		assertThat(fileHeader).isNotNull();
		assertThat(fileHeader.getFileName()).isEqualTo(FILE_NAME);
	}

	@Test
	public void testGetFileHeaderWithExactMatchCaseSensitive() throws ZipException {
		final String lowerCaseFile = FILE_NAME.toLowerCase();
		final String upperCaseFile = FILE_NAME.toUpperCase();
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Arrays.asList(
				generateFileHeader(lowerCaseFile),
				generateFileHeader(upperCaseFile)
		));
		zipModel.setCentralDirectory(centralDirectory);

		assertThat(centralDirectory.getFileHeaders()).hasSize(2);

		FileHeader fileHeaderLower = HeaderUtil.getFileHeader(zipModel, lowerCaseFile);
		assertThat(fileHeaderLower).isNotNull();
		assertThat(fileHeaderLower.getFileName()).isEqualTo(lowerCaseFile);

		FileHeader fileHeaderUpper = HeaderUtil.getFileHeader(zipModel, upperCaseFile);
		assertThat(fileHeaderUpper).isNotNull();
		assertThat(fileHeaderUpper.getFileName()).isEqualTo(upperCaseFile);
	}

	@Test
	public void testGetFileHeaderWithWindowsFileSeparator() throws ZipException {
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Arrays.asList(
				generateFileHeader(FILE_NAME),
				generateFileHeader("SOME_OTHER_NAME\\")
		));
		zipModel.setCentralDirectory(centralDirectory);

		FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, "SOME_OTHER_NAME\\");
		assertThat(fileHeader).isNotNull();
		assertThat(fileHeader.getFileName()).isEqualTo("SOME_OTHER_NAME\\");
	}

	@Test
	public void testGetFileHeaderWithUnixFileSeparator() throws ZipException {
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Arrays.asList(
				generateFileHeader(FILE_NAME),
				generateFileHeader("SOME_OTHER_NAME/")
		));
		zipModel.setCentralDirectory(centralDirectory);

		FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, "SOME_OTHER_NAME/");
		assertThat(fileHeader).isNotNull();
		assertThat(fileHeader.getFileName()).isEqualTo("SOME_OTHER_NAME/");
	}

	@Test
	public void testGetFileHeaderWithoutAMatch() throws ZipException {
		ZipModel zipModel = new ZipModel();
		CentralDirectory centralDirectory = new CentralDirectory();
		centralDirectory.setFileHeaders(Arrays.asList(
				generateFileHeader(FILE_NAME),
				generateFileHeader("SOME_OTHER_NAME")
		));
		zipModel.setCentralDirectory(centralDirectory);

		assertThat(HeaderUtil.getFileHeader(zipModel, "SHOULD_NOT_EXIST")).isNull();
	}

	@Test
	public void testDecodeStringWithCharsetForUtf8() {
		String utf8StringToEncode = "asdäüöö";
		byte[] utf8EncodedBytes = utf8StringToEncode.getBytes(InternalZipConstants.CHARSET_UTF_8);

		assertThat(HeaderUtil.decodeStringWithCharset(utf8EncodedBytes, true, null)).isEqualTo(utf8StringToEncode);
	}

	@Test
	public void testDecodeStringWithCharsetWithCharsetGBKForChineseString() {
		String chineseStringToEncode = "写記立要";
		byte[] gbkEncodedBytes = chineseStringToEncode.getBytes(Charset.forName("GBK"));

		String decodedString = HeaderUtil.decodeStringWithCharset(gbkEncodedBytes, false, Charset.forName("GBK"));

		assertThat(decodedString).isEqualTo(chineseStringToEncode);
	}

	@Test
	public void testDecodeStringWithCharsetWithoutUtf8AndWithEnglishChars() {
		String plainString = "asdasda234234";
		byte[] plainEncodedBytes = plainString.getBytes();

		assertThat(HeaderUtil.decodeStringWithCharset(plainEncodedBytes, false, null)).isEqualTo(plainString);
	}

	@Test
	public void testDecodeStringWithCharsetWithISO8859AndFinnishChars() {
		String finnishString = "asdäüöö";
		byte[] plainEncodedBytes = finnishString.getBytes(StandardCharsets.ISO_8859_1);

		assertThat(HeaderUtil.decodeStringWithCharset(plainEncodedBytes, false, StandardCharsets.ISO_8859_1)).isEqualTo(finnishString);
	}

	@Test
	public void testDecodeStringWithCharsetWithUTF8CharsetAndKoreanChars() {
		String koreanString = "가나다";
		byte[] plainEncodedBytes = koreanString.getBytes(InternalZipConstants.CHARSET_UTF_8);

		assertThat(HeaderUtil.decodeStringWithCharset(plainEncodedBytes, true, null)).isEqualTo(koreanString);
	}

	@Test
	public void testDecodeStringWithCharsetWithNullCharsetAndEnglishChars() {
		String englishString = "asdasda234234";
		byte[] plainEncodedBytes = englishString.getBytes();

		assertThat(HeaderUtil.decodeStringWithCharset(plainEncodedBytes, false, null)).isEqualTo(englishString);
	}

	@Test
	public void testGetFileHeadersUnderDirectoryWhenNotDirectoryReturnsEmptyList() {
		List<FileHeader> allFileHeaders = generateFileHeaderWithFileNames("header", 5);

		assertThat(HeaderUtil.getFileHeadersUnderDirectory(allFileHeaders, "some_name")).isEmpty();
	}

	@Test
	public void testGetFileHeadersUnderDirectoryReturnsFileHeadersUnderDirectory() {
		List<FileHeader> allFileHeaders = generateFileHeaderWithFileNames("some_name/header", 5);
		allFileHeaders.add(generateFileHeader("some_name/"));
		allFileHeaders.add(generateFileHeader("some_other_name.txt"));

		List<FileHeader> filHeadersUnderDirectory = HeaderUtil.getFileHeadersUnderDirectory(allFileHeaders, "some_name/");
		assertThat(filHeadersUnderDirectory).hasSize(6);
		for (FileHeader fileHeader : filHeadersUnderDirectory) {
			assertThat(fileHeader)
					.withFailMessage("file header with name some_other_name.txt should not exist")
					.isNotEqualTo("some_other_name.txt");
		}
	}

	@Test
	public void testGetUncompressedSizeOfAllFileHeaders() {
		FileHeader fileHeader1 = generateFileHeader("1");
		fileHeader1.setUncompressedSize(1000);
		FileHeader fileHeader2 = generateFileHeader("2");
		fileHeader2.setUncompressedSize(2000);
		FileHeader fileHeader3 = generateFileHeader("3");
		Zip64ExtendedInfo zip64ExtendedInfo = new Zip64ExtendedInfo();
		zip64ExtendedInfo.setUncompressedSize(3000);
		fileHeader3.setZip64ExtendedInfo(zip64ExtendedInfo);
		fileHeader3.setUncompressedSize(0);
		List<FileHeader> fileHeaders = Arrays.asList(fileHeader1, fileHeader2, fileHeader3);

		assertThat(HeaderUtil.getTotalUncompressedSizeOfAllFileHeaders(fileHeaders)).isEqualTo(6000);
	}

	@Test
	public void testGetOffsetStartOfCentralDirectoryForZip64Format() {
		long offsetCentralDirectory = InternalZipConstants.ZIP_64_SIZE_LIMIT + 100;
		ZipModel zipModel = new ZipModel();
		zipModel.setZip64Format(true);
		Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = new Zip64EndOfCentralDirectoryRecord();
		zip64EndOfCentralDirectoryRecord.setOffsetStartCentralDirectoryWRTStartDiskNumber(offsetCentralDirectory);
		zipModel.setZip64EndOfCentralDirectoryRecord(zip64EndOfCentralDirectoryRecord);

		assertThat(HeaderUtil.getOffsetStartOfCentralDirectory(zipModel)).isEqualTo(offsetCentralDirectory);
	}

	@Test
	public void testGetOffsetStartOfCentralDirectoryForNonZip64Format() {
		long offsetStartOfCentralDirectory = InternalZipConstants.ZIP_64_SIZE_LIMIT - 100;
		ZipModel zipModel = new ZipModel();
		zipModel.setZip64Format(false);
		EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = new EndOfCentralDirectoryRecord();
		endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(offsetStartOfCentralDirectory);
		zipModel.setEndOfCentralDirectoryRecord(endOfCentralDirectoryRecord);

		assertThat(HeaderUtil.getOffsetStartOfCentralDirectory(zipModel)).isEqualTo(offsetStartOfCentralDirectory);
	}

	private List<FileHeader> generateFileHeaderWithFileNamesWithEmptyAndNullFileNames(String fileNamePrefix, int numberOfEntriesToAdd) {
		List<FileHeader> fileHeaders = generateFileHeaderWithFileNames(fileNamePrefix, numberOfEntriesToAdd);
		fileHeaders.add(generateFileHeader(""));
		fileHeaders.add(generateFileHeader(null));
		return fileHeaders;
	}

	private List<FileHeader> generateFileHeaderWithFileNames(String fileNamePrefix, int numberOfEntriesToAdd) {
		List<FileHeader> fileHeaders = new ArrayList<>();
		for (int i = 0; i < numberOfEntriesToAdd; i++) {
			fileHeaders.add(generateFileHeader(fileNamePrefix + i));
		}
		return fileHeaders;
	}

	private FileHeader generateFileHeader(String fileName) {
		FileHeader fileHeader = new FileHeader();
		fileHeader.setFileName(fileName);
		return fileHeader;
	}
}
