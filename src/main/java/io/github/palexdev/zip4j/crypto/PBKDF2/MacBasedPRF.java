/*
 * Copyright 2010 Srikanth Reddy Lingala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.palexdev.zip4j.crypto.PBKDF2;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/*
 * Source referred from Matthias Gartner's PKCS#5 implementation -
 * see http://rtner.de/software/PBKDF2.html
 */

public class MacBasedPRF implements PRF {
	private Mac mac;
	private int hLen;
	private String macAlgorithm;

	public MacBasedPRF(String macAlgorithm) {
		this.macAlgorithm = macAlgorithm;
		try {
			mac = Mac.getInstance(macAlgorithm);
			hLen = mac.getMacLength();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] doFinal(byte[] M) {
		return mac.doFinal(M);
	}

	public byte[] doFinal() {
		return mac.doFinal();
	}

	public int getHLen() {
		return hLen;
	}

	public void init(byte[] P) {
		try {
			mac.init(new SecretKeySpec(P, macAlgorithm));
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(byte[] U) {
		try {
			mac.update(U);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(byte[] U, int start, int len) {
		try {
			mac.update(U, start, len);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		}
	}
}
