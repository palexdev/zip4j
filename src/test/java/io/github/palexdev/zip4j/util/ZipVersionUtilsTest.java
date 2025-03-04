package io.github.palexdev.zip4j.util;

import io.github.palexdev.zip4j.headers.VersionNeededToExtract;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.model.enums.CompressionMethod;
import io.github.palexdev.zip4j.model.enums.EncryptionMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipVersionUtilsTest {

	private static final String ACTUAL_OS = System.getProperty("os.name");

	private RawIO rawIO = new RawIO();

	@BeforeEach
	public void setup() {
		System.setProperty("os.name", "linux");
	}

	@AfterEach
	public void cleanup() {
		System.setProperty("os.name", ACTUAL_OS);
	}

	@Test
	public void testDetermineVersionMadeByUnix() {
		assertThat(ZipVersionUtils.determineVersionMadeBy(new ZipParameters(), rawIO)).isEqualTo(819);
	}

	@Test
	public void testDetermineVersionMadeByWindows() {
		changeOsSystemPropertyToWindows();
		assertThat(ZipVersionUtils.determineVersionMadeBy(new ZipParameters(), rawIO)).isEqualTo(51);
	}

	@Test
	public void testDetermineVersionMadeByWindowsAndUnixModeOn() {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setUnixMode(true);
		assertThat(ZipVersionUtils.determineVersionMadeBy(zipParameters, rawIO)).isEqualTo(819);
	}

	@Test
	public void testDetermineVersionNeededToExtractDefault() {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.STORE);
		assertThat(ZipVersionUtils.determineVersionNeededToExtract(zipParameters)).isEqualTo(VersionNeededToExtract.DEFAULT);
	}

	@Test
	public void testDetermineVersionNeededToExtractDefalte() {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
		assertThat(ZipVersionUtils.determineVersionNeededToExtract(zipParameters)).isEqualTo(VersionNeededToExtract.DEFLATE_COMPRESSED);
	}

	@Test
	public void testDetermineVersionNeededToExtractZip64() {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setEntrySize(InternalZipConstants.ZIP_64_SIZE_LIMIT + 10);
		assertThat(ZipVersionUtils.determineVersionNeededToExtract(zipParameters)).isEqualTo(VersionNeededToExtract.ZIP_64_FORMAT);
	}

	@Test
	public void testDetermineVersionNeededToExtractAES() {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setEncryptFiles(true);
		zipParameters.setEncryptionMethod(EncryptionMethod.AES);
		assertThat(ZipVersionUtils.determineVersionNeededToExtract(zipParameters)).isEqualTo(VersionNeededToExtract.AES_ENCRYPTED);
	}

	private void changeOsSystemPropertyToWindows() {
		System.setProperty("os.name", "windows");
	}
}