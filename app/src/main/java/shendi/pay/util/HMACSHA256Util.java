package shendi.pay.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 消息认证码
 * 创建时间：2025/3/7
 *
 * @author 砷碲
 */
public class HMACSHA256Util {

    /**
     * 根据密钥与数据生成消息认证码
     * @param priKey    密钥
     * @param data      数据
     * @return  消息认证码
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static String hmacSHA256(byte[] priKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(priKey, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data));
    }

}
