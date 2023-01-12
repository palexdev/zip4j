package io.github.palexdev.zip4j;

import io.github.palexdev.zip4j.model.FileHeader;
import io.github.palexdev.zip4j.model.Zip4jConfig;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.model.enums.AesKeyStrength;
import io.github.palexdev.zip4j.model.enums.EncryptionMethod;
import io.github.palexdev.zip4j.testutils.TestUtils;
import io.github.palexdev.zip4j.util.InternalZipConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static io.github.palexdev.zip4j.testutils.TestUtils.getTestFileFromResources;
import static io.github.palexdev.zip4j.util.InternalZipConstants.USE_UTF8_FOR_PASSWORD_ENCODING_DECODING;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIT {

	protected static final char[] PASSWORD = "test123!".toCharArray();
	protected static final List<File> FILES_TO_ADD = Arrays.asList(
			getTestFileFromResources("sample_text1.txt"),
			getTestFileFromResources("sample_text_large.txt"),
			getTestFileFromResources("sample.pdf")
	);
	protected static final Charset CHARSET_MS_932 = Charset.forName("Ms932");
	protected static final Charset CHARSET_GBK = Charset.forName("GBK");
	protected static final Charset CHARSET_CP_949 = Charset.forName("Cp949");

	protected File generatedZipFile;
	protected File outputFolder;

	protected static Path basePath;
	protected static Path temporaryFolder;

	@BeforeAll
	public static void setup() throws IOException {
		String testDir = System.getenv("ZIP4J_TEST_DIR");
		if (testDir != null) {
			basePath = Path.of(testDir);
			if (!Files.exists(basePath)) Files.createDirectory(basePath);
		} else {
			basePath = Path.of(System.getProperty("java.io.tmpdir"));
			if (!Files.exists(basePath)) throw new IOException("System temp folder not found");
		}
	}

	@AfterAll
	public static void dispose() throws IOException {
		if (temporaryFolder != null && Files.exists(temporaryFolder)) {
			Files.walk(temporaryFolder)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			temporaryFolder = null;
		}
	}

	@BeforeEach
	public void before() throws IOException {
		if (temporaryFolder != null) {
			Files.walk(temporaryFolder)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
		temporaryFolder = Files.createTempDirectory(basePath, "zip4j");

		generatedZipFile = Files.createFile(temporaryFolder.resolve("output.zip")).toFile();
		outputFolder = Files.createFile(temporaryFolder.resolve("output")).toFile();
		cleanupDirectory(temporaryFolder.toFile());
	}

	protected ZipParameters createZipParameters(EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setEncryptFiles(true);
		zipParameters.setEncryptionMethod(encryptionMethod);
		zipParameters.setAesKeyStrength(aesKeyStrength);
		return zipParameters;
	}

	protected void verifyFileHeadersContainsFiles(List<FileHeader> fileHeaders, List<String> fileNames) {
		for (String fileName : fileNames) {
			boolean fileFound = false;
			for (FileHeader fileHeader : fileHeaders) {
				if (fileHeader.getFileName().equals(fileName)) {
					fileFound = true;
					break;
				}
			}

			assertThat(fileFound).as("File with name %s not found in zip file", fileName).isTrue();
		}
	}

	protected File getTestArchiveFromResources(String archiveName) {
		return TestUtils.getTestArchiveFromResources(archiveName);
	}

	protected void cleanupOutputFolder() {
		cleanupDirectory(outputFolder);
	}

	protected Zip4jConfig buildDefaultConfig() {
		return buildConfig(null);
	}

	protected Zip4jConfig buildConfig(Charset charset) {
		return new Zip4jConfig(charset, InternalZipConstants.BUFF_SIZE, USE_UTF8_FOR_PASSWORD_ENCODING_DECODING);
	}

	private void cleanupDirectory(File directory) {
		File[] allTempFiles = directory.listFiles();
		if (allTempFiles == null) {
			return;
		}
		for (File file : allTempFiles) {
			try {
				boolean delete = file.delete();
				if (!delete) throw new RuntimeException("Could not clean up directory. Error deleting file: " + file);
			} catch (Exception ex) {
				throw new RuntimeException("Could not clean up directory. Error deleting file: " + file, ex);
			}
		}
	}
}
