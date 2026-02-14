package com.cqcloud.platform.utils;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import cn.hutool.core.util.HexUtil;

/**
 * SM4加解密工具类
 *
 * @author weimeilayer@gmail.com
 * @date 💓💕 2025-12-03 18:42:25💓💕
 */
public class Sm4Utils {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private static final String ENCODING = "UTF-8";

	public static final String ALGORITHM_NAME = "SM4";

	// 加密算法/分组加密模式/分组填充方式
	// PKCS5Padding-以8个字节为一组进行分组加密
	// 定义分组加密模式使用：PKCS5Padding
	public static final String ALGORITHM_NAME_ECB_PADDING = "SM4/ECB/PKCS5Padding";

	// 128-32位16进制；256-64位16进制
	public static final int DEFAULT_KEY_SIZE = 128;

	/**
	 * 生成 SM4 Cipher
	 * @param algorithmName
	 * @param mode
	 * @param key
	 * @return
	 * @throws Exception
	 */
	private static Cipher generateEcbCipher(String algorithmName, int mode, byte[] key) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithmName, BouncyCastleProvider.PROVIDER_NAME);
		Key sm4Key = new SecretKeySpec(key, ALGORITHM_NAME);
		cipher.init(mode, sm4Key);
		return cipher;
	}

	/**
	 * SM4加密
	 * @param hexKey
	 * @param paramStr
	 * @return
	 * @throws Exception
	 */
	public static String encryptEcb(byte[] hexKey, String paramStr) throws Exception {
		String cipherText = null;
		byte[] keyData = hexKey;
		byte[] srcData = paramStr.getBytes(ENCODING);
		byte[] cipherArray = encrypt_Ecb_Padding(keyData.toString(), srcData);
		cipherText = Base64.getEncoder().encodeToString(cipherArray);
		return cipherText;
	}

	/**
	 * SM4解密
	 * @param secret
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt_Ecb_Padding(String secret, byte[] data) throws Exception {
		String encrypt = encrypt(secret);
		byte[] keys = HexUtil.decodeHex(encrypt);
		Cipher cipher = generateEcbCipher(ALGORITHM_NAME_ECB_PADDING, Cipher.ENCRYPT_MODE, keys);
		return cipher.doFinal(data);
	}

	/**
	 * SM4解密
	 * @param hexKey
	 * @param cipherText
	 * @return
	 * @throws Exception
	 */
	public static String decryptEcb(byte[] hexKey, String cipherText) throws Exception {
		String decryptStr = "";
		byte[] keyData = hexKey;
		byte[] cipherData = Base64.getDecoder().decode(cipherText);
		byte[] srcData = decrypt_Ecb_Padding(keyData.toString(), cipherData);
		decryptStr = new String(srcData, ENCODING);
		return decryptStr;
	}

	/**
	 * SM4解密
	 * @param secret
	 * @param cipherText
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt_Ecb_Padding(String secret, byte[] cipherText) throws Exception {
		String encrypt = encrypt(secret);
		byte[] keys = HexUtil.decodeHex(encrypt);
		Cipher cipher = generateEcbCipher(ALGORITHM_NAME_ECB_PADDING, Cipher.DECRYPT_MODE, keys);
		return cipher.doFinal(cipherText);
	}

	/**
	 * SM4验证
	 * @param hexKey
	 * @param cipherText
	 * @param paramStr
	 * @return
	 * @throws Exception
	 */
	public static boolean verifyEcb(byte[] hexKey, String cipherText, String paramStr) throws Exception {
		boolean flag = false;
		byte[] keyData = hexKey;
		byte[] cipherData = Base64.getDecoder().decode(cipherText);
		byte[] decryptData = decrypt_Ecb_Padding(keyData.toString(), cipherData);
		byte[] srcData = paramStr.getBytes(ENCODING);
		flag = Arrays.equals(decryptData, srcData);
		return flag;
	}

	/**
	 * 获取16进制字符串的MD5值
	 * @param input
	 * @return
	 */
	public static String encrypt(String input) {
		try {
			// 创建 MD5 摘要算法实例
			MessageDigest md = MessageDigest.getInstance("MD5");

			// 将输入字符串转换为字节数组
			byte[] inputBytes = input.getBytes();

			// 计算摘要
			byte[] hashBytes = md.digest(inputBytes);

			// 将字节数组转换为十六进制字符串
			StringBuilder hexString = new StringBuilder();
			for (byte hashByte : hashBytes) {
				String hex = Integer.toHexString(0xFF & hashByte);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();

		}
		catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	/**
	 * 对字符串进行Base64编码加密
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String encryptToBase64(String data, String key) throws Exception {
		byte[] encryptedBytes = encrypt_Ecb_Padding(key, data.getBytes(ENCODING));
		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	/**
	 * 对字符串进行Base64解密
	 * @param encryptedData
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String decryptFromBase64(String encryptedData, String key) throws Exception {
		byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
		byte[] decryptedBytes = decrypt_Ecb_Padding(key, encryptedBytes);
		return new String(decryptedBytes, ENCODING);
	}

}