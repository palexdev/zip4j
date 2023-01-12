package io.github.palexdev.zip4j.io.inputstream;

class StoreInputStream extends DecompressedInputStream {

	public StoreInputStream(CipherInputStream<?> cipherInputStream) {
		super(cipherInputStream);
	}
}
