package io.github.palexdev.zip4j.io.outputstream;

import io.github.palexdev.zip4j.AbstractIT;
import io.github.palexdev.zip4j.ZipFile;
import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.io.inputstream.ZipInputStream;
import io.github.palexdev.zip4j.model.FileHeader;
import io.github.palexdev.zip4j.model.LocalFileHeader;
import io.github.palexdev.zip4j.model.Zip4jConfig;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.model.enums.AesKeyStrength;
import io.github.palexdev.zip4j.model.enums.AesVersion;
import io.github.palexdev.zip4j.model.enums.CompressionMethod;
import io.github.palexdev.zip4j.model.enums.EncryptionMethod;
import io.github.palexdev.zip4j.testutils.TestUtils;
import io.github.palexdev.zip4j.util.BitUtils;
import io.github.palexdev.zip4j.util.FileUtils;
import io.github.palexdev.zip4j.util.InternalZipConstants;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.github.palexdev.zip4j.testutils.TestUtils.getTestFileFromResources;
import static io.github.palexdev.zip4j.testutils.ZipFileVerifier.verifyZipFileByExtractingAllFiles;
import static io.github.palexdev.zip4j.util.FileUtils.*;
import static io.github.palexdev.zip4j.util.InternalZipConstants.MIN_BUFF_SIZE;
import static io.github.palexdev.zip4j.util.InternalZipConstants.USE_UTF8_FOR_PASSWORD_ENCODING_DECODING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZipOutputStreamIT extends AbstractIT {

	@Test
	public void testConstructorThrowsExceptionWhenBufferSizeIsLessThanExpected() throws IOException {
		OutputStream outputStream = new ByteArrayOutputStream();
		Zip4jConfig zip4jConfig = new Zip4jConfig(null, InternalZipConstants.MIN_BUFF_SIZE - 1, USE_UTF8_FOR_PASSWORD_ENCODING_DECODING);
		assertThrows(
				IllegalArgumentException.class,
				() -> new ZipOutputStream(outputStream, null, zip4jConfig, null),
				"Buffer size cannot be less than " + MIN_BUFF_SIZE + " bytes"
		);

	}

	@Test
	public void testZipOutputStreamStoreWithoutEncryption() throws IOException {
		testZipOutputStream(CompressionMethod.STORE, false, null, null, null);
	}

	@Test
	public void testZipOutputStreamStoreWithStandardEncryption() throws IOException {
		testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.ZIP_STANDARD, null, null);
	}

	@Test
	public void testZipOutputStreamStoreWithAES256V1() throws IOException {
		testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, AesVersion.ONE);
	}

	@Test
	public void testZipOutputStreamStoreWithAES128V2() throws IOException {
		testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, AesVersion.TWO);
	}

	@Test
	public void testZipOutputStreamStoreWithAES256V2() throws IOException {
		testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, AesVersion.TWO);
	}

	@Test
	public void testZipOutputStreamDeflateWithoutEncryption() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, false, null, null, null);
	}

	@Test
	public void testZipOutputStreamDeflateWithoutEncryptionAndKoreanFilename() throws IOException {
		List<File> filesToAdd = new ArrayList<>();
		filesToAdd.add(getTestFileFromResources("가나다.abc"));

		testZipOutputStream(CompressionMethod.DEFLATE, false, null, null, null, true,
				filesToAdd, CHARSET_CP_949);
	}

	@Test
	public void testZipOutputStreamDeflateWithStandardEncryption() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, null);
	}

	@Test
	public void testZipOutputStreamDeflateWithStandardEncryptionWhenModifiedFileTimeNotSet()
			throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, null, false);
	}

	@Test
	public void testZipOutputStreamDeflateWithAES128V1() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, AesVersion.ONE);
	}

	@Test
	public void testZipOutputStreamDeflateWithAES128() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, AesVersion.TWO);
	}

	@Test
	public void testZipOutputStreamDeflateWithAES256() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, AesVersion.TWO);
	}

	@Test
	public void testZipOutputStreamDeflateWithNullVersionUsesV2() throws IOException {
		testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, null);
	}

	@Test
	public void testZipOutputStreamThrowsExceptionWhenEntrySizeNotSetForStoreCompression() throws IOException {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.STORE);
		assertThrows(
				IllegalArgumentException.class,
				() -> {
					try (ZipOutputStream zos = initializeZipOutputStream(false, InternalZipConstants.CHARSET_UTF_8)) {
						for (File fileToAdd : FILES_TO_ADD) {
							zipParameters.setLastModifiedFileTime(fileToAdd.lastModified());
							zipParameters.setFileNameInZip(fileToAdd.getName());
							zos.putNextEntry(zipParameters);
						}
					}
				},
				"uncompressed size should be set for zip entries of compression type store"
		);
	}

	@Test
	public void testAddCommentToEntryWithUtf8Charset() throws IOException {
		testAddCommentToEntryWithCharset(StandardCharsets.UTF_8, "COMMENT_");
	}

	@Test
	public void testAddCommentToEntryWithNullCharset() throws IOException {
		testAddCommentToEntryWithCharset(null, "COMMENT_ÜÖ_");
	}

	@Test
	public void testAddCommentToEntryWithGBKCharset() throws IOException {
		testAddCommentToEntryWithCharset(Charset.forName("GBK"), "fff - 副本");
	}

	@Test
	public void testAddCommentToZipOutputStreamWithUtf8Charset() throws IOException {
		testAddCommentToZipOutputStreamWithCharset(StandardCharsets.UTF_8, "SOME_COMMENT");
	}

	@Test
	public void testAddCommentToZipOutputStreamWithNullCharsetUsesUtf8() throws IOException {
		testAddCommentToZipOutputStreamWithCharset(null, "SOME_COMMENT_WITH_NO_CHARSET");
	}

	@Test
	public void testAddCommentToZipOutputStreamWithGBKCharset() throws IOException {
		testAddCommentToZipOutputStreamWithCharset(Charset.forName("GBK"), "fff - 副本");
	}

	@Test
	public void testAddCommentToZipOutputStreamAfterClosingThrowsException() throws IOException {
		assertThrows(
				IOException.class,
				() -> {
					ZipParameters zipParameters = new ZipParameters();
					ZipOutputStream zos = initializeZipOutputStream(false, InternalZipConstants.CHARSET_UTF_8);
					for (File fileToAdd : FILES_TO_ADD) {
						zipParameters.setLastModifiedFileTime(fileToAdd.lastModified());
						zipParameters.setFileNameInZip(fileToAdd.getName());
						zos.putNextEntry(zipParameters);
					}
					zos.close();
					zos.setComment("SOME_COMMENT");
				},
				"Stream is closed"
		);

	}

	@Test
	public void testDefaultFileAttributes() throws IOException {
		List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
		filesToAdd.add(TestUtils.getTestFileFromResources("/"));
		byte[] buff = new byte[4096];
		int readLen;
		ZipParameters zipParameters = new ZipParameters();

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
			for (File fileToAdd : filesToAdd) {
				if (fileToAdd.isDirectory()) {
					zipParameters.setFileNameInZip(fileToAdd.getName() + "/");
					zipOutputStream.putNextEntry(zipParameters);
					zipOutputStream.closeEntry();
					continue;
				}

				zipParameters.setFileNameInZip(fileToAdd.getName());
				zipOutputStream.putNextEntry(zipParameters);
				InputStream inputStream = new FileInputStream(fileToAdd);
				while ((readLen = inputStream.read(buff)) != -1) {
					zipOutputStream.write(buff, 0, readLen);
				}

				inputStream.close();
				zipOutputStream.closeEntry();
			}
		}

		verifyDefaultFileAttributes();
	}

	@Test
	public void testCreatingZipWithoutClosingEntryManuallySuccessfullyClosesEntry() throws IOException {
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
			File fileToAdd = getTestFileFromResources("file_PDF_1MB.pdf");
			byte[] buffer = new byte[InternalZipConstants.BUFF_SIZE];
			int readLen;
			ZipParameters zipParameters = new ZipParameters();
			zipParameters.setFileNameInZip(fileToAdd.getName());
			zipOutputStream.putNextEntry(zipParameters);
			try (InputStream inputStream = new FileInputStream(fileToAdd)) {
				while ((readLen = inputStream.read(buffer)) != -1) {
					zipOutputStream.write(buffer, 0, readLen);
				}
			}
		}

		verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
	}

	@Test
	public void testCreateZipFileWithDirectoriesAndExtendedLocalFileHeaderIsSuccessful() throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
			putNextEntryAndCloseEntry(zos, "dir1/");
			putNextEntryAndCloseEntry(zos, "dir2/");
		}

		ZipFile zipFile = new ZipFile(generatedZipFile);
		List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		for (FileHeader fileHeader : fileHeaders) {
			assertThat(fileHeader.isDataDescriptorExists()).isFalse();
		}
		verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
	}


	@Test
	public void testPutNextEntryWithEmptyFileNameInZipParameters() throws IOException {
		ZipParameters zParams = new ZipParameters();
		zParams.setFileNameInZip("");

		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
			try {
				zos.putNextEntry(zParams);
				fail("Suppose to throw an exception");
			} catch (Exception ex) {
				assertThat(ex).isInstanceOf(IllegalArgumentException.class);
				assertThat(ex).hasMessageContaining("fileNameInZip is null or empty");
			}
		}
	}

	@Test
	public void testPutNextEntryWithNullFileNameInZipParameters() throws IOException {
		ZipParameters zParams = new ZipParameters();
		zParams.setFileNameInZip(null);

		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
			try {
				zos.putNextEntry(zParams);
				fail("Suppose to throw an exception");
			} catch (Exception ex) {
				assertThat(ex).isInstanceOf(IllegalArgumentException.class);
				assertThat(ex).hasMessageContaining("fileNameInZip is null or empty");
			}
		}
	}

	@Test
	public void testLastModifiedTimeIsSetWhenItIsNotExplicitlySet() throws IOException {
		long currentTime = System.currentTimeMillis();
		ByteArrayOutputStream zip = new ByteArrayOutputStream();

		try (ZipOutputStream zos = new ZipOutputStream(zip)) {
			ZipParameters params = new ZipParameters();
			params.setFileNameInZip("test");
			zos.putNextEntry(params);
			zos.closeEntry();
		}

		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip.toByteArray()))) {
			LocalFileHeader fileHeader = zis.getNextEntry();
			long zipTime = fileHeader.getLastModifiedTimeEpoch();
			assertThat(currentTime).isLessThan(zipTime + 2000);
		}
	}

	private void testZipOutputStream(CompressionMethod compressionMethod, boolean encrypt,
	                                 EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength,
	                                 AesVersion aesVersion)
			throws IOException {
		testZipOutputStream(compressionMethod, encrypt, encryptionMethod, aesKeyStrength, aesVersion, true);
	}

	private void testZipOutputStream(CompressionMethod compressionMethod, boolean encrypt,
	                                 EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength,
	                                 AesVersion aesVersion, boolean setLastModifiedTime)
			throws IOException {
		testZipOutputStream(compressionMethod, encrypt, encryptionMethod, aesKeyStrength, aesVersion, setLastModifiedTime,
				FILES_TO_ADD, InternalZipConstants.CHARSET_UTF_8);
	}

	private void testZipOutputStream(CompressionMethod compressionMethod, boolean encrypt,
	                                 EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength,
	                                 AesVersion aesVersion, boolean setLastModifiedTime, List<File> filesToAdd,
	                                 Charset charset)
			throws IOException {

		ZipParameters zipParameters = buildZipParameters(compressionMethod, encrypt, encryptionMethod, aesKeyStrength);
		zipParameters.setAesVersion(aesVersion);

		byte[] buff = new byte[4096];
		int readLen;

		try (ZipOutputStream zos = initializeZipOutputStream(encrypt, charset)) {
			for (File fileToAdd : filesToAdd) {

				if (zipParameters.getCompressionMethod() == CompressionMethod.STORE) {
					zipParameters.setEntrySize(fileToAdd.length());
				}

				if (setLastModifiedTime) {
					zipParameters.setLastModifiedFileTime(fileToAdd.lastModified());
				}
				zipParameters.setFileNameInZip(fileToAdd.getName());
				zos.putNextEntry(zipParameters);

				try (InputStream inputStream = new FileInputStream(fileToAdd)) {
					while ((readLen = inputStream.read(buff)) != -1) {
						zos.write(buff, 0, readLen);
					}
				}
				zos.closeEntry();
			}
		}
		verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, filesToAdd.size(), true, charset);
		verifyEntries();
	}

	private void testAddCommentToEntryWithCharset(Charset charset, String fileCommentPrefix) throws IOException {
		ZipParameters zipParameters = new ZipParameters();
		byte[] buff = new byte[4096];
		int readLen;

		List<File> filesToAdd = FILES_TO_ADD;
		try (ZipOutputStream zos = initializeZipOutputStream(false, charset)) {
			for (int i = 0; i < filesToAdd.size(); i++) {
				File fileToAdd = filesToAdd.get(i);
				zipParameters.setFileNameInZip(fileToAdd.getName());

				if (i == 0) {
					zipParameters.setFileComment(fileCommentPrefix + i);
				} else {
					zipParameters.setFileComment(null);
				}

				zos.putNextEntry(zipParameters);

				try (InputStream inputStream = new FileInputStream(fileToAdd)) {
					while ((readLen = inputStream.read(buff)) != -1) {
						zos.write(buff, 0, readLen);
					}
				}
				zos.closeEntry();
			}
		}
		verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, filesToAdd.size(), true, charset);
		verifyEntries();
		verifyFileEntryComment(fileCommentPrefix, charset);
	}

	private void testAddCommentToZipOutputStreamWithCharset(Charset charset, String comment) throws IOException {
		ZipParameters zipParameters = new ZipParameters();
		byte[] buff = new byte[4096];
		int readLen;

		List<File> filesToAdd = FILES_TO_ADD;
		try (ZipOutputStream zos = initializeZipOutputStream(false, charset)) {
			for (int i = 0; i < filesToAdd.size(); i++) {
				File fileToAdd = filesToAdd.get(i);
				zipParameters.setFileNameInZip(fileToAdd.getName());

				zos.putNextEntry(zipParameters);

				try (InputStream inputStream = new FileInputStream(fileToAdd)) {
					while ((readLen = inputStream.read(buff)) != -1) {
						zos.write(buff, 0, readLen);
					}
				}
				zos.closeEntry();
			}
			zos.setComment(comment);
		}
		verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, filesToAdd.size(), true, charset);
		verifyEntries();
		verifyZipComment(comment, charset);
	}

	private void verifyEntries() throws ZipException {
		ZipFile zipFile = new ZipFile(generatedZipFile);
		for (FileHeader fileHeader : zipFile.getFileHeaders()) {
			byte[] generalPurposeBytes = fileHeader.getGeneralPurposeFlag();
			assertThat(BitUtils.isBitSet(generalPurposeBytes[0], 3)).isTrue();

			if (fileHeader.isEncrypted()
					&& fileHeader.getEncryptionMethod().equals(EncryptionMethod.AES)) {

				if (fileHeader.getAesExtraDataRecord().getAesVersion().equals(AesVersion.TWO)) {
					assertThat(fileHeader.getCrc()).isZero();
				} else if (fileHeader.getCompressedSize() > 0) {
					assertThat(fileHeader.getCrc()).isNotZero();
				}
			}
		}
	}

	private ZipOutputStream initializeZipOutputStream(boolean encrypt, Charset charset) throws IOException {
		FileOutputStream fos = new FileOutputStream(generatedZipFile);

		if (encrypt) {
			return new ZipOutputStream(fos, PASSWORD, charset);
		}

		return new ZipOutputStream(fos, null, charset);
	}

	private ZipParameters buildZipParameters(CompressionMethod compressionMethod, boolean encrypt,
	                                         EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(compressionMethod);
		zipParameters.setEncryptionMethod(encryptionMethod);
		zipParameters.setAesKeyStrength(aesKeyStrength);
		zipParameters.setEncryptFiles(encrypt);
		return zipParameters;
	}

	private void verifyFileEntryComment(String commentPrefix, Charset charset) throws IOException {
		ZipFile zipFile = initializeZipFileWithCharset(charset);
		List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		for (int i = 0; i < fileHeaders.size(); i++) {
			FileHeader fileHeader = fileHeaders.get(i);
			if (i == 0) {
				assertThat(fileHeader.getFileComment()).isEqualTo(commentPrefix + i);
			} else {
				assertThat(fileHeader.getFileComment()).isNull();
			}
		}
	}

	private void verifyZipComment(String expectedComment, Charset charset) throws IOException {
		ZipFile zipFile = initializeZipFileWithCharset(charset);
		assertThat(zipFile.getComment()).isEqualTo(expectedComment);
	}

	private void verifyDefaultFileAttributes() throws ZipException {
		ZipFile zipFile = new ZipFile(generatedZipFile);
		List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		byte[] emptyAttributes = new byte[4];

		for (FileHeader fileHeader : fileHeaders) {
			if (isUnix() || isMac()) {
				if (fileHeader.isDirectory()) {
					assertThat(fileHeader.getExternalFileAttributes()).isEqualTo(FileUtils.DEFAULT_POSIX_FOLDER_ATTRIBUTES);
				} else {
					assertThat(fileHeader.getExternalFileAttributes()).isEqualTo(FileUtils.DEFAULT_POSIX_FILE_ATTRIBUTES);
				}
			} else if (isWindows() && fileHeader.isDirectory()) {
				assertThat(BitUtils.isBitSet(fileHeader.getExternalFileAttributes()[0], 4)).isTrue();
			} else {
				assertThat(fileHeader.getExternalFileAttributes()).isEqualTo(emptyAttributes);
			}
		}
	}

	private ZipFile initializeZipFileWithCharset(Charset charset) {
		ZipFile zipFile = new ZipFile(generatedZipFile);

		if (charset != null) {
			zipFile.setCharset(charset);
		}

		return zipFile;
	}

	private void putNextEntryAndCloseEntry(ZipOutputStream zipOutputStream, String fileName) throws IOException {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setFileNameInZip(fileName);
		zipOutputStream.putNextEntry(zipParameters);
		zipOutputStream.closeEntry();
	}
}
