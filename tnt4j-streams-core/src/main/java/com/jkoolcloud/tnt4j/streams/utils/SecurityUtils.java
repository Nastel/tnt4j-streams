/*
 * Copyright 2014-2019 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.utils;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * Security related utility API used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class SecurityUtils {
	private static final char[] PASSWORD = "nastelAutoPilotM6-898923784628937".toCharArray(); // NON-NLS
	private static final byte[] SALT = { //
			(byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, // 0-7
	};
	private static final String SEC_ALGO = "PBEWithMD5AndDES"; // NON-NLS
	private static final String PASS_MASK = "******"; // NON-NLS

	/**
	 * Encrypts provided plain text password and encodes it using Base64.
	 *
	 * @param passStr
	 *            plain text password string to encrypt
	 * @return encrypted and Base64 encoded password, or {@code null} - if provided password string is {@code null}
	 *
	 * @throws GeneralSecurityException
	 *             if password encryption fails because of unsupported algorithm, invalid parameters or invalid key
	 * @see Utils#base64EncodeStr(byte[])
	 */
	public static String encryptPass(String passStr) throws GeneralSecurityException {
		if (passStr == null) {
			return passStr;
		}

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SEC_ALGO);
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		Cipher pbeCipher = Cipher.getInstance(SEC_ALGO);
		pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));

		return Utils.base64EncodeStr(pbeCipher.doFinal(passStr.getBytes()));
	}

	/**
	 * Decrypts provided password string.
	 *
	 * @param passStr
	 *            password string to decrypt
	 * @return decrypted plain text password string, or {@code null} - if provided password string is {@code null}
	 *
	 * @throws GeneralSecurityException
	 *             if password decryption fails because of unsupported algorithm, invalid parameters or invalid key
	 * @see Utils#base64Decode(String)
	 */
	public static String decryptPass(String passStr) throws GeneralSecurityException {
		if (passStr == null) {
			return passStr;
		}

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SEC_ALGO);
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		Cipher pbeCipher = Cipher.getInstance(SEC_ALGO);
		pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));

		return Utils.getString(pbeCipher.doFinal(Utils.base64Decode(passStr)));
	}

	/**
	 * Tries to decrypt provided password string. If decryption fails or provided password is {@code null} - original
	 * password string is returned.
	 * 
	 * @param passStr
	 *            password string
	 * @return decrypted plain text password string, or original password string value if decryption fails or provided
	 *         password string is {@code null}
	 *
	 * @see #decryptPass(String)
	 */
	public static String getPass(String passStr) {
		try {
			return decryptPass(passStr);
		} catch (Throwable exc) {
			return passStr;
		}
	}

	/**
	 * Checks is provided password string is encoded by Base64 and tries to decrypt it. If decryption fails or provided
	 * password is {@code null} - original password string is returned.
	 *
	 * @param passStr
	 *            password string
	 * @return decrypted password string, or original password string value if decryption fails or provided password
	 *         string is {@code null}
	 *
	 * @see #decryptPass(String)
	 */
	public static String getPass2(String passStr) {
		if (passStr != null && Base64.isBase64(passStr)) {
			try {
				return decryptPass(passStr);
			} catch (Throwable exc) {
			}
		}

		return passStr;
	}

	/**
	 * Encrypts provided password string by using {@link #encryptPass(String)} method. If encryption method throws
	 * exception, then password mask {@value #PASS_MASK} is returned.
	 *
	 * @param passStr
	 *            plain text password string
	 * @return encrypted password string or mask {@value #PASS_MASK} if password can't be encrypted
	 *
	 * @see #encryptPass(String)
	 */
	public static String encrypt(String passStr) {
		try {
			return encryptPass(passStr);
		} catch (Throwable exc) {
			// return passStr;
			return PASS_MASK;
		}
	}

	private static final String PARAM_ENCRYPT = "-encrypt"; // NON-NLS
	private static final String PARAM_ENCRYPT2 = "-e"; // NON-NLS
	private static final String PARAM_DECRYPT = "-decrypt"; // NON-NLS
	private static final String PARAM_DECRYPT2 = "-d"; // NON-NLS
	private static final String PARAM_HELP1 = "-h"; // NON-NLS
	private static final String PARAM_HELP2 = "-?"; // NON-NLS

	/**
	 * Main entry point to execute Security related operations.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams Security utils command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-e | -encrypt &lt;plain_text_password&gt;</td>
	 *            <td>Encrypts provided &lt;plain_text_password&gt; argument string</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-d | -decrypt &lt;encrypted_password&gt;</td>
	 *            <td>Decrypts provided &lt;encrypted_password&gt; argument string</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-h | -?</td>
	 *            <td>(optional) Print usage</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) {
		System.out.println();

		boolean argsValid = processArgs(args);
	}

	private static boolean processArgs(String... args) {
		String cmd = null;
		String param = null;

		for (String arg : args) {
			arg = StringUtils.trimToNull(arg);
			if (StringUtils.isEmpty(arg)) {
				continue;
			}

			if (arg.equals(PARAM_ENCRYPT) || arg.equals(PARAM_ENCRYPT2)) {
				if (StringUtils.isNotEmpty(cmd)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"SecurityUtils.invalid.args"));
					printUsage();
					return false;
				}

				cmd = arg;
			} else if (arg.equals(PARAM_DECRYPT) || arg.equals(PARAM_DECRYPT2)) {
				if (StringUtils.isNotEmpty(cmd)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"SecurityUtils.invalid.args"));
					printUsage();
					return false;
				}

				cmd = arg;
			} else if (PARAM_HELP1.equals(arg) || PARAM_HELP2.equals(arg)) {
				printUsage();
				return false;
			} else if (arg.startsWith("-")) { // NON-NLS
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.invalid.argument", arg));
				printUsage();
				return false;
			} else {
				param = arg;
			}
		}

		if (StringUtils.isEmpty(cmd)) {
			printUsage();
			return false;
		}

		switch (cmd) {
		case PARAM_ENCRYPT:
		case PARAM_ENCRYPT2:
			try {
				String encryptedPass = encryptPass(param);

				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"SecurityUtils.encrypted.pass", encryptedPass));
			} catch (Exception exc) {
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"SecurityUtils.encrypt.failed", param));
				exc.printStackTrace();
			}
			break;
		case PARAM_DECRYPT:
		case PARAM_DECRYPT2:
			try {
				String decryptedPass = decryptPass(param);

				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"SecurityUtils.decrypted.pass", decryptedPass));
			} catch (Exception exc) {
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"SecurityUtils.decrypt.failed", param));
				exc.printStackTrace();
			}
			break;
		default:
			printUsage();
		}

		return true;
	}

	private static void printUsage() {
		System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "SecurityUtils.help"));
	}
}
