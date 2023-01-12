package io.github.palexdev.zip4j.testutils;

import io.github.palexdev.zip4j.AbstractIT;
import io.github.palexdev.zip4j.io.outputstream.ZipOutputStream;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.util.FileUtils;
import io.github.palexdev.zip4j.util.InternalZipConstants;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {

	private static final String TEST_FILES_FOLDER_NAME = "test-files";
	private static final String TEST_ARCHIVES_FOLDER_NAME = "test-archives";

	public static File getTestFileFromResources(String fileName) {
		return getFileFromResources(TEST_FILES_FOLDER_NAME, fileName);
	}

	public static File getTestArchiveFromResources(String fileName) {
		return getFileFromResources(TEST_ARCHIVES_FOLDER_NAME, fileName);
	}

	/**
	 * Splits files with extension .001, .002, etc
	 *
	 * @param fileToSplit file to be split
	 * @param splitLength the length of each split file
	 * @return File - first split file
	 * @throws IOException if any exception occurs dealing with streams
	 */
	public static File splitFileWith7ZipFormat(File fileToSplit, File outputFolder, long splitLength) throws IOException {
		if (splitLength < InternalZipConstants.MIN_SPLIT_LENGTH) {
			throw new IllegalArgumentException("split length less than minimum allowed split length of " + InternalZipConstants.MIN_SPLIT_LENGTH);
		}

		int splitCounter = 0;
		byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
		int readLen = 0;
		long numberOfBytesWrittenInThisPart = 0;

		try (InputStream inputStream = new FileInputStream(fileToSplit)) {
			OutputStream outputStream = startNext7ZipSplitStream(fileToSplit, outputFolder, splitCounter);
			splitCounter++;

			while ((readLen = inputStream.read(buff)) != -1) {
				if (numberOfBytesWrittenInThisPart + readLen > splitLength) {
					int numberOfBytesToWriteInThisCounter = (int) (splitLength - numberOfBytesWrittenInThisPart);
					outputStream.write(buff, 0, numberOfBytesToWriteInThisCounter);
					outputStream.close();
					outputStream = startNext7ZipSplitStream(fileToSplit, outputFolder, splitCounter);
					splitCounter++;
					outputStream.write(buff, numberOfBytesToWriteInThisCounter, readLen - numberOfBytesToWriteInThisCounter);
					numberOfBytesWrittenInThisPart = readLen - numberOfBytesToWriteInThisCounter;
				} else {
					outputStream.write(buff, 0, readLen);
					numberOfBytesWrittenInThisPart += readLen;
				}
			}

			outputStream.close();
		}

		return getFileNameFor7ZipSplitIndex(fileToSplit, outputFolder, 0);
	}

	public static void copyFile(File sourceFile, File destinationFile) throws IOException {
		Files.copy(sourceFile.toPath(), destinationFile.toPath());
	}

	public static void copyFileToFolder(File sourceFile, File outputFolder) throws IOException {
		File destinationFile = new File(outputFolder.getAbsolutePath(), sourceFile.getName());
		copyFile(sourceFile, destinationFile);
	}

	public static void copyFileToFolder(File sourceFile, File outputFolder, int numberOfCopiesToMake) throws IOException {
		for (int i = 0; i < numberOfCopiesToMake; i++) {
			File destinationFile = new File(outputFolder.getAbsolutePath(), i + ".pdf");
			copyFile(sourceFile, destinationFile);
		}
	}

	public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
		if (!destinationDirectory.exists()) {
			destinationDirectory.mkdir();
		}
		for (String f : sourceDirectory.list()) {
			copyDirectoryCompatibilityMode(new File(sourceDirectory, f), new File(destinationDirectory, f));
		}
	}

	public static void copyDirectoryCompatibilityMode(File source, File destination) throws IOException {
		if (source.isDirectory()) {
			copyDirectory(source, destination);
		} else {
			copyFile(source, destination);
		}
	}

	public static void createZipFileWithZipOutputStream(File zipFile, List<File> filesToAdd) throws IOException {

		byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
		int readLen = -1;
		ZipParameters zipParameters = new ZipParameters();

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			for (File fileToAdd : filesToAdd) {
				zipParameters.setFileNameInZip(fileToAdd.getName());
				zipOutputStream.putNextEntry(zipParameters);

				try (InputStream inputStream = new FileInputStream(fileToAdd)) {
					while ((readLen = inputStream.read(buff)) != -1) {
						zipOutputStream.write(buff, 0, readLen);
					}
				}

				zipOutputStream.closeEntry();
			}
		}
	}

	public static File generateFileOfSize(Path temporaryFolder, long fileSize) throws IOException {
		File outputFile = Files.createTempFile(temporaryFolder, "zip4j-test", "").toFile();
		byte[] b = new byte[8 * InternalZipConstants.BUFF_SIZE];
		Random random = new Random();
		long bytesWritten = 0;
		int bufferWriteLength;

		try (OutputStream outputStream = new FileOutputStream(outputFile)) {
			while (bytesWritten < fileSize) {
				random.nextBytes(b);
				bufferWriteLength = bytesWritten + b.length > fileSize ? ((int) (fileSize - bytesWritten)) : b.length;
				outputStream.write(b, 0, bufferWriteLength);
				bytesWritten += bufferWriteLength;
			}
		}

		return outputFile;
	}

	public static File createSymlink(File targetFile, File rootFolder) throws IOException {
		Path link = Paths.get(rootFolder.getAbsolutePath(), "symlink.link");
		Files.createSymbolicLink(link, targetFile.toPath());
		return link.toFile();
	}

	public static List<String> getFileNamesOfFiles(List<File> files) {
		List<String> fileNames = new ArrayList<>();
		if (files.isEmpty()) {
			return fileNames;
		}

		for (File file : files) {
			fileNames.add(file.getName());
		}
		return fileNames;
	}

	private static OutputStream startNext7ZipSplitStream(File sourceFile, File outputFolder, int index) throws IOException {
		File outputFile = getFileNameFor7ZipSplitIndex(sourceFile, outputFolder, index);
		return new FileOutputStream(outputFile);
	}

	private static File getFileNameFor7ZipSplitIndex(File sourceFile, File outputFolder, int index) throws IOException {
		return new File(outputFolder.getCanonicalPath() + File.separator + sourceFile.getName()
				+ FileUtils.getNextNumberedSplitFileCounterAsExtension(index));
	}

	private static File getFileFromResources(String parentFolder, String fileName) {
		try {
			Class<AbstractIT> klass = AbstractIT.class;
			URL res = klass.getResource(parentFolder + "/" + fileName);
			return new File(res.toURI());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load resource: " + fileName, ex);
		}
	}
}
