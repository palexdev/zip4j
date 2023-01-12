package io.github.palexdev.zip4j.crypto.PBKDF2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BinToolsTest {

	@Test
	public void bin2hexForValidInputReturnsValidHex() {
		final byte[] b = {(byte) 112};
		assertThat(BinTools.bin2hex(b)).isEqualTo("70");
	}

	@Test
	public void bin2hexForEmptyInputReturnsEmptyString() {
		assertThat(BinTools.bin2hex(new byte[]{})).isEqualTo("");
	}

	@Test
	public void bin2hexForNullInputReturnsEmptyString() {
		assertThat(BinTools.bin2hex(null)).isEqualTo("");
	}

	@Test
	public void bin2hexForNullInputReturnsEmptyArray() {
		assertThat(BinTools.hex2bin(null)).isEqualTo(new byte[]{});
	}

	@Test
	public void hex2binForInvalidInputOutputIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> BinTools.hex2bin("foo"));
	}

	@Test
	public void hex2binCharacterInputOutputPositive() {
		assertThat(BinTools.hex2bin('A')).isEqualTo(10);
	}

	@Test
	public void hex2binInvalidInputOutputIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> BinTools.hex2bin('\u013c'));
	}
}
