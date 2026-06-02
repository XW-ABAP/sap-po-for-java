import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TokenFetcher {

    public static void main(String[] args) {
        System.out.println("========== 开始获取泛微 Token ==========");
        try {
            // 忽略 HTTPS 证书校验 (仅供本地 VS Code 测试使用，放入 PO 时不需要这段)
            trustAllHosts(); 
            
            String token = getEcologyToken();
            System.out.println("========== 获取成功 ==========");
            System.out.println("最终获取到的 Token: " + token);
        } catch (Exception e) {
            System.err.println("========== 获取失败 ==========");
            e.printStackTrace();
        }
    }

    /**
     * 获取 Token 的核心逻辑
     */
    public static String getEcologyToken() throws Exception {
        String finalToken = "";
        
        // ==========================================
        // 第一步：调用 /auth/regist 获取 spk (公钥) 和 secret
        // ==========================================
        System.out.println("-> 正在执行第一步: 调用 regist 接口...");
        URL url1 = new URL("{host}/api/ec/dev/auth/regist");
        HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
        conn1.setRequestMethod("POST");
        
        conn1.setRequestProperty("appid", "dangan_sap");
        conn1.setRequestProperty("Cookie", "ecology_JSessionid=aaa4gn_Zpp2Idf2ViNc6z");
        conn1.setRequestProperty("User-Agent", "PostmanRuntime/7.37.3");
        conn1.setRequestProperty("Accept", "*/*");
        conn1.setRequestProperty("Connection", "keep-alive");
        conn1.setDoOutput(true);
        
        if (conn1.getResponseCode() != 200) {
            throw new Exception("第一步失败，HTTP 状态码: " + conn1.getResponseCode());
        }
        
        BufferedReader in1 = new BufferedReader(new InputStreamReader(conn1.getInputStream(), "UTF-8"));
        StringBuilder response1 = new StringBuilder();
        String inputLine1;
        while ((inputLine1 = in1.readLine()) != null) { 
            response1.append(inputLine1); 
        }
        in1.close();
        String json1 = response1.toString();
        System.out.println("   regist 接口返回: " + json1);
        
        // 使用正则提取 spk 和 secret
        Pattern patternSecret = Pattern.compile("\"secret\"\\s*:\\s*\"([^\"]+)\"");
        Pattern patternSpk = Pattern.compile("\"spk\"\\s*:\\s*\"([^\"]+)\"");
        Matcher mSecret = patternSecret.matcher(json1);
        Matcher mSpk = patternSpk.matcher(json1);
        
        String secret = "";
        String spk = "";
        if (mSecret.find() && mSpk.find()) {
            secret = mSecret.group(1).trim();
            spk = mSpk.group(1).replaceAll("[\\s\\r\\n]", ""); 
            
            System.out.println("   提取 secret 成功: " + secret);
            // --- 修改这里：不再截取前30位，直接打印完整的 spk ---
            System.out.println("   提取 spk 成功: " + spk);
        } else {
            throw new Exception("解析失败：未在 regist 报文中找到 secret 或 spk 字段。");
        }

        // ==========================================
        // 第二步：使用 spk 对 secret 和 userid 进行 RSA 加密
        // ==========================================
        System.out.println("-> 正在执行第二步: RSA 加密...");
        byte[] keyBytes = Base64.getDecoder().decode(spk);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        // 1. 加密 secret
        byte[] encryptedBytes = cipher.doFinal(secret.getBytes("UTF-8"));
        String encryptedSecret = Base64.getEncoder().encodeToString(encryptedBytes);
        System.out.println("   加密后的 secret: " + encryptedSecret);

        // 2. 加密 userid
        String userId = "1";
        byte[] encryptedUserIdBytes = cipher.doFinal(userId.getBytes("UTF-8"));
        String encryptedUserId = Base64.getEncoder().encodeToString(encryptedUserIdBytes);
        System.out.println("   加密后的 userid: " + encryptedUserId);

        // ==========================================
        // 第三步：调用 /auth/applytoken 获取最终 Token
        // ==========================================
        System.out.println("-> 正在执行第三步: 调用 applytoken 接口...");
        URL url2 = new URL("{host}/api/ec/dev/auth/applytoken");
        HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
        conn2.setRequestMethod("POST");
        
        conn2.setRequestProperty("User-Agent", "PostmanRuntime/7.37.3");
        conn2.setRequestProperty("Accept", "*/*");
        conn2.setRequestProperty("appid", "dangan_sap");
        conn2.setRequestProperty("secret", encryptedSecret); 
        conn2.setRequestProperty("userid", encryptedUserId); 
        conn2.setRequestProperty("Cookie", "ecology_JSessionid=aaa4gn_Zpp2Idf2ViNc6z");
        conn2.setDoOutput(true);

        if (conn2.getResponseCode() != 200) {
            throw new Exception("第三步失败，HTTP 状态码: " + conn2.getResponseCode());
        }

        BufferedReader in2 = new BufferedReader(new InputStreamReader(conn2.getInputStream(), "UTF-8"));
        StringBuilder response2 = new StringBuilder();
        String inputLine2;
        while ((inputLine2 = in2.readLine()) != null) { 
            response2.append(inputLine2); 
        }
        in2.close();
        String json2 = response2.toString();
        System.out.println("   applytoken 接口返回: " + json2);

        Pattern patternToken = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
        Matcher mToken = patternToken.matcher(json2);
        
        if (mToken.find()) {
            finalToken = mToken.group(1);
        } else {
            throw new Exception("解析失败：未在 applytoken 报文中找到 token 字段。");
        }

        return finalToken;
    }

    /**
     * 信任所有 HTTPS 证书（仅用于本地绕过 SSL 校验）
     */
    public static void trustAllHosts() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}