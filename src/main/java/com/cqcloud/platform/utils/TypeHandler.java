package com.cqcloud.platform.utils;

import com.cqcloud.platform.constant.CommonConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 自定义类型处理器
 *
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕2026年2月12日🐬🐇 💓💕
 */
public class TypeHandler extends BaseTypeHandler<String> {

	// 注意：这里的密钥是原始字符串，不是MD5值，Sm4Utils内部会进行MD5处理
	private static final String SM4_KEY = CommonConstants.SM4_PRIVATE_KEY;

	/**
	 * 插入数据时，对数据进行加密
	 */
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
			throws SQLException {
		try {
			ps.setString(i, Sm4Utils.encryptToBase64(parameter, SM4_KEY));
		}
		catch (Exception e) {
			throw new SQLException("SM4加密失败", e);
		}
	}

	// 默认需要解密的字段
	private static final List<String> DEFAULT_FIELDS = Arrays.asList("username", "phone", "nickname", "realName",
			"name", "cardNo", "email", "wxOpenid", "miniOpenid", "ykbTalkOpenid", "dingTalkOpenid");

	/**
	 * 默认使用默认字段
	 * @param obj
	 */
	public static void decrypt(Object obj) {
		decrypt(obj, DEFAULT_FIELDS.toArray(new String[0]));
	}

	/**
	 * 使用指定字段
	 */
	public static void decrypt(Object obj, String... fieldNames) {
		Optional.ofNullable(obj)
			.ifPresent(o -> Arrays.stream(fieldNames).forEach(fieldName -> decryptField(o, fieldName)));
	}

	/**
	 * 解密字段
	 * @param obj
	 * @param fieldName
	 */
	private static void decryptField(Object obj, String fieldName) {
		try {
			// 获取字段
			Field field = getField(obj.getClass(), fieldName);
			if (field == null) {
				return;
			}
			// 设置字段可访问
			field.setAccessible(true);
			String value = (String) field.get(obj);
			// 解密
			Optional.ofNullable(value).filter(StringUtils::isNotBlank).ifPresent(v -> {
				try {
					String toDecrypt = v;
					String prefix = null;
					// 公共处理：只要包含@，就取最后一部分解密
					int atIndex = v.lastIndexOf('@');
					// 包含@符号
					if (atIndex >= 0) {
						// 包含@
						prefix = v.substring(0, atIndex + 1);
						// @后面的部分
						toDecrypt = v.substring(atIndex + 1);
					}
					// 有@符号
					String decrypted = Sm4Utils.decryptFromBase64(toDecrypt, SM4_KEY);
					// 有前缀加前缀，没有直接设置
					field.set(obj, prefix != null ? prefix + decrypted : decrypted);
				}
				catch (Exception e) {
					throw new RuntimeException(fieldName + "解密失败！", e);
				}
			});
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(fieldName + "访问失败！", e);
		}
	}

	/**
	 * 递归获取字段（支持父类）
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	private static Field getField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException e) {
			if (clazz.getSuperclass() != null) {
				return getField(clazz.getSuperclass(), fieldName);
			}
			return null;
		}
	}

	/**
	 * 通用加密方法
	 * @param getter 获取原始值
	 * @param setter 设置加密值
	 */
	public static <T> void encryptField(T dto, Function<T, String> getter, BiConsumer<T, String> setter) {
		Optional.ofNullable(getter.apply(dto))
			.filter(StringUtils::isNotBlank)
			.map(String::trim)
			.map(TypeHandler::encryptText)
			.ifPresent(encrypted -> setter.accept(dto, encrypted));
	}

	/**
	 * 查询数据时，对数据进行解密
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return Optional.ofNullable(rs.getString(columnName)).map(v -> {
			try {
				return Sm4Utils.decryptFromBase64(v, SM4_KEY);
			}
			catch (Exception e) {
				try {
					throw new SQLException("SM4解密失败", e);
				}
				catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).orElse(null);
	}

	/**
	 * 获取结果集时，对数据进行解密
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return Optional.ofNullable(rs.getString(columnIndex)).map(v -> {
			try {
				return Sm4Utils.decryptFromBase64(v, SM4_KEY);
			}
			catch (Exception e) {
				try {
					throw new SQLException("SM4解密失败", e);
				}
				catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).orElse(null);
	}

	/**
	 * 获取存储过程结果集时，对数据进行解密
	 * @param cs
	 * @return
	 * @throws SQLException
	 */
	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return Optional.ofNullable(cs.getString(columnIndex)).map(v -> {
			try {
				return Sm4Utils.decryptFromBase64(v, SM4_KEY);
			}
			catch (Exception e) {
				try {
					throw new SQLException("SM4解密失败", e);
				}
				catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).orElse(null);
	}

	/**
	 * 单个字符串加密
	 * @param plainText 明文字符串
	 * @return 密文字符串
	 */
	public static String encryptText(String plainText) {
		if (StringUtils.isBlank(plainText)) {
			return plainText;
		}
		try {
			return Sm4Utils.encryptToBase64(plainText, SM4_KEY);
		}
		catch (Exception e) {
			throw new RuntimeException("字符串加密失败", e);
		}
	}

	/**
	 * 单个字段加密（加密为纯密文，不加前缀）
	 * @param obj 对象
	 * @param fieldName 字段名
	 */
	public static void encryptField(Object obj, String fieldName) {
		try {
			Field field = getField(obj.getClass(), fieldName);
			if (field == null) {
				return;
			}

			field.setAccessible(true);
			String value = (String) field.get(obj);

			Optional.ofNullable(value).filter(StringUtils::isNotBlank).ifPresent(v -> {
				try {
					// ⭐ 直接加密明文，不处理@前缀
					String encrypted = Sm4Utils.encryptToBase64(v, SM4_KEY);
					field.set(obj, encrypted);
				}
				catch (Exception e) {
					throw new RuntimeException("字段[" + fieldName + "]加密失败", e);
				}
			});
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException("字段[" + fieldName + "]访问失败", e);
		}
	}

	/**
	 * 单个对象加密（使用默认字段）
	 */
	public static void encrypt(Object obj) {
		encrypt(obj, DEFAULT_FIELDS.toArray(new String[0]));
	}

	/**
	 * 单个对象加密（指定字段）
	 */
	public static void encrypt(Object obj, String... fieldNames) {
		Optional.ofNullable(obj)
			.ifPresent(o -> Arrays.stream(fieldNames).forEach(fieldName -> encryptField(o, fieldName)));
	}

	/**
	 * 测试方法
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// 500228200005201314
		final String data = "weimeilayer@gmail.com";
		final String secret = SM4_KEY;

		// 测试新方法
		String encrypted = Sm4Utils.encryptToBase64(data, secret);
		System.out.println("新方法加密: " + encrypted);

		String decrypted = Sm4Utils.decryptFromBase64(encrypted, secret);
		System.out.println("新方法解密: " + decrypted);

		// 测试老方法做对比
		final Base64.Encoder encoder = Base64.getEncoder();
		final String encode = encoder.encodeToString(Sm4Utils.encrypt_Ecb_Padding(secret, data.getBytes()));
		System.out.println("老方法加密: " + encode);
	}

}