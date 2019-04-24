package com.Cardinal.NetworkAdmin.Crypto;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

public class CryptoManager {

	private static final String ALGORITHM = "RSA";
	private static HashMap<InetAddress, KeyPair> KEYS = new HashMap<InetAddress, KeyPair>();
	private static HashMap<InetAddress, PublicKey> PUBLIC_KEYS = new HashMap<InetAddress, PublicKey>();

	public static void removeKeys(InetAddress address) {
		KEYS.remove(address);
		PUBLIC_KEYS.remove(address);
	}

	public static byte[] encrypt(InetAddress address, byte[] data) throws InvalidKeyException, InvalidKeySpecException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		PublicKey key = PUBLIC_KEYS.get(address);
		return encrypt(key, data);
	}

	public static byte[] decrypt(InetAddress address, byte[] data)
			throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException,
			BadPaddingException, NoSuchPaddingException, NoSuchProviderException {
		return decrypt(getPrivateKey(address), data);
	}

	public static void setPublicKey(InetAddress address, byte[] key)
			throws InvalidKeySpecException, NoSuchAlgorithmException {
		PUBLIC_KEYS.put(address, decodeKey(key));
	}

	public static PublicKey getPublicKey(InetAddress address) throws NoSuchAlgorithmException, NoSuchProviderException {
		return KEYS.containsKey(address) ? KEYS.get(address).getPublic()
				: KEYS.put(address, generateKeyPair()).getPublic();
	}

	private static PrivateKey getPrivateKey(InetAddress address)
			throws NoSuchAlgorithmException, NoSuchProviderException {
		return KEYS.containsKey(address) ? KEYS.get(address).getPrivate()
				: KEYS.put(address, generateKeyPair()).getPrivate();
	}

	public static boolean checkCredentials(InetAddress address, byte[] credentials)
			throws InvalidKeyException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException {

		HANDLEByReference phUser = new HANDLEByReference();
		if (!Advapi32.INSTANCE.LogonUser(getLocalAdmin(), InetAddress.getLocalHost().getHostName(),
				new String(decrypt(getPrivateKey(address), credentials)), WinBase.LOGON32_LOGON_NETWORK,
				WinBase.LOGON32_PROVIDER_DEFAULT, phUser)) {
			return false;
		}

		return true;
	}

	private static byte[] encrypt(PublicKey publicKey, byte[] inputData)
			throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException {

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);

		byte[] encryptedBytes = cipher.doFinal(inputData);

		return encryptedBytes;
	}

	private static byte[] decrypt(PrivateKey privateKey, byte[] inputData)
			throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchPaddingException {

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);

		byte[] decryptedBytes = cipher.doFinal(inputData);

		return decryptedBytes;
	}

	private static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);

		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

		// 512 is keysize
		keyGen.initialize(512, random);

		KeyPair generateKeyPair = keyGen.generateKeyPair();
		return generateKeyPair;
	}

	private static PublicKey decodeKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
		return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(encoded));
	}

	private static String getLocalAdmin() {
		File f = new File("C:/Users");
		return f.list((dir, name) -> name.toLowerCase().contains("student"))[0];
	}
}
