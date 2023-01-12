package io.github.palexdev.zip4j.util;

import io.github.palexdev.zip4j.AbstractIT;
import io.github.palexdev.zip4j.ZipFile;
import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.io.inputstream.NumberedSplitFileInputStream;
import io.github.palexdev.zip4j.io.inputstream.SplitFileInputStream;
import io.github.palexdev.zip4j.io.inputstream.ZipStandardSplitFileInputStream;
import io.github.palexdev.zip4j.model.FileHeader;
import io.github.palexdev.zip4j.model.ZipModel;
import io.github.palexdev.zip4j.testutils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UnzipUtilIT extends AbstractIT {

	@Test
	public void testCreateZipInputStream() throws ZipException, IOException {
		ZipFile zipFile = createZipFile();
		ZipModel zipModel = createZipModel();
		FileHeader fileHeader = zipFile.getFileHeaders().get(1);
		File extractedFile = Files.createTempFile(temporaryFolder, "zip4j_test", "").toFile();

		try (InputStream inputStream = UnzipUtil.createZipInputStream(zipModel, fileHeader, "password".toCharArray());
		     OutputStream outputStream = new FileOutputStream(extractedFile)) {
			byte[] b = new byte[InternalZipConstants.BUFF_SIZE];
			int readLen = 0;

			while ((readLen = inputStream.read(b)) != -1) {
				outputStream.write(b, 0, readLen);
			}
		}

		assertThat(extractedFile.length()).isEqualTo(TestUtils.getTestFileFromResources("sample_text_large.txt").length());
	}

	@Test
	public void testApplyFileAttributes() {
		byte[] externalFileAttributes = new byte[]{12, 34, 0, 0};
		long currentTime = System.currentTimeMillis();
		FileHeader fileHeader = new FileHeader();
		fileHeader.setExternalFileAttributes(externalFileAttributes);
		fileHeader.setLastModifiedTime(currentTime);

		File file = mock(File.class);
		Path path = mock(Path.class);
		when(file.toPath()).thenReturn(path);

		MockedStatic<FileUtils> mockStatic = mockStatic(FileUtils.class);

		UnzipUtil.applyFileAttributes(fileHeader, file);

		verify(FileUtils.class);
		FileUtils.setFileLastModifiedTime(path, currentTime);

		verify(FileUtils.class);
		FileUtils.setFileAttributes(path, externalFileAttributes);
		if (!mockStatic.isClosed()) mockStatic.close();
	}

	@Test
	public void testApplyFileFileAttributesSetsLastModifiedTimeWithoutNio() {
		byte[] externalFileAttributes = new byte[]{12, 34, 0, 0};
		long currentTime = System.currentTimeMillis();
		FileHeader fileHeader = new FileHeader();
		fileHeader.setExternalFileAttributes(externalFileAttributes);
		fileHeader.setLastModifiedTime(currentTime);

		File file = mock(File.class);
		Path path = mock(Path.class);
		when(file.toPath()).thenThrow(new NoSuchMethodError("No method"));

		MockedStatic<FileUtils> mockStatic = mockStatic(FileUtils.class);

		UnzipUtil.applyFileAttributes(fileHeader, file);

		verify(FileUtils.class, never());
		FileUtils.setFileLastModifiedTime(path, currentTime);

		verify(FileUtils.class, never());
		FileUtils.setFileAttributes(path, externalFileAttributes);

		verify(FileUtils.class);
		FileUtils.setFileLastModifiedTimeWithoutNio(file, currentTime);
		if (!mockStatic.isClosed()) mockStatic.close();
	}

	@Test
	public void testCreateSplitInputStreamForNumberedSplitZipReturnsInstance() throws IOException {
		String zipFileName = "somename.zip.001";
		File zipFile = Files.createFile(temporaryFolder.resolve(zipFileName)).toFile();
		ZipModel zipModel = createZipModel();
		zipModel.setZipFile(zipFile);

		SplitFileInputStream splitInputStream = UnzipUtil.createSplitInputStream(zipModel);

		assertThat(splitInputStream).isInstanceOf(NumberedSplitFileInputStream.class);
	}

	@Test
	public void testCreateSplitInputStreamForNonNumberedSplitZipReturnsInstance() throws IOException {
		Files.createFile(temporaryFolder.resolve(generatedZipFile.getName()));
		ZipModel zipModel = createZipModel();

		SplitFileInputStream splitInputStream = UnzipUtil.createSplitInputStream(zipModel);

		assertThat(splitInputStream).isInstanceOf(ZipStandardSplitFileInputStream.class);
	}

	private ZipFile createZipFile() throws ZipException {
		ZipFile zipFile = new ZipFile(generatedZipFile, "password".toCharArray());
		zipFile.addFiles(Arrays.asList(
				TestUtils.getTestFileFromResources("sample_text1.txt"),
				TestUtils.getTestFileFromResources("sample_text_large.txt")
		));
		return zipFile;
	}

	private ZipModel createZipModel() {
		ZipModel zipModel = new ZipModel();
		zipModel.setZipFile(generatedZipFile);
		zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(0);
		zipModel.setSplitArchive(false);
		return zipModel;
	}

}