public String GetToken(String var1, Container container) throws StreamTransformationException{
    String finalToken = "";
    String encryptedUserId = "";
    
    try {
        // ==========================================
        // 前置：获取当前 PO 服务器的主机名并动态分配 Base URL
        // ==========================================
        String localHostName = java.net.InetAddress.getLocalHost().getHostName().toLowerCase();
        container.getTrace().addInfo("-> 当前所在服务器 HostName: " + localHostName);

        String baseUrl = "";
        if (localHostName.contains("podev")) {
            // 【开发机】
            baseUrl = "https://oa_dev/api/ec/dev"; // <--- 请核对开发机 URL 前缀
            container.getTrace().addInfo("-> 识别为开发环境，使用 URL: " + baseUrl);
            
        } else if (localHostName.contains("jhza1hptt01")) {
            // 【测试机】
            baseUrl = "https://oa_dev";   
            container.getTrace().addInfo("-> 识别为测试环境，使用 URL: " + baseUrl);
            
        } else if (localHostName.contains("jhza1wp2p01")) {
            // 【正式机】
            baseUrl = "https://oa_dev"; // <--- 请核对正式机 URL 前缀
            container.getTrace().addInfo("-> 识别为正式环境，使用 URL: " + baseUrl);
            
        } else {
            // 【兜底逻辑】如果都不匹配，默认走测试机
            baseUrl = "https://oa_dev";
            container.getTrace().addWarning("-> 未知环境: " + localHostName + "，默认使用测试环境 URL");
        }

        // ==========================================
        // 第一步：调用 /auth/regist 获取 spk (公钥) 和 secret
        // ==========================================
        java.net.URL url1 = new java.net.URL(baseUrl + "/auth/regist");
        java.net.HttpURLConnection conn1 = (java.net.HttpURLConnection) url1.openConnection();
        conn1.setRequestMethod("POST");
        
        // 伪装请求头，保持和 Postman 完全一致
        conn1.setRequestProperty("appid", "dangan_sap");
        conn1.setRequestProperty("Cookie", "ecology_JSessionid=aaa4gn_Zpp2Idf2ViNc6z");
        conn1.setRequestProperty("User-Agent", "PostmanRuntime/7.37.3");
        conn1.setRequestProperty("Accept", "*/*");
        conn1.setRequestProperty("Connection", "keep-alive");
        conn1.setDoOutput(true);
        
        if (conn1.getResponseCode() != 200) {
            throw new StreamTransformationException("第一步失败，HTTP 状态码: " + conn1.getResponseCode());
        }
        
        java.io.BufferedReader in1 = new java.io.BufferedReader(new java.io.InputStreamReader(conn1.getInputStream(), "UTF-8"));
        StringBuilder response1 = new StringBuilder();
        String inputLine1;
        while ((inputLine1 = in1.readLine()) != null) { 
            response1.append(inputLine1); 
        }
        in1.close();
        String json1 = response1.toString();
        
        // 使用正则提取 spk 和 secret
        java.util.regex.Pattern patternSecret = java.util.regex.Pattern.compile("\"secret\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Pattern patternSpk = java.util.regex.Pattern.compile("\"spk\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher mSecret = patternSecret.matcher(json1);
        java.util.regex.Matcher mSpk = patternSpk.matcher(json1);
        
        String secret = "";
        String spk = "";
        if (mSecret.find() && mSpk.find()) {
            secret = mSecret.group(1).trim();
            // 净化 spk，剔除所有可能导致 Base64 解析失败的空格和回车
            spk = mSpk.group(1).replaceAll("[\\s\\r\\n]", ""); 
        } else {
            throw new StreamTransformationException("解析失败：未在 regist 报文中找到 secret 或 spk 字段。");
        }

        // ==========================================
        // 第二步：使用 spk 对 secret 和 userid 进行 RSA 加密
        // ==========================================
        byte[] keyBytes = java.util.Base64.getDecoder().decode(spk);
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
        
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        
        // 1. 加密 secret
        byte[] encryptedBytes = cipher.doFinal(secret.getBytes("UTF-8"));
        String encryptedSecret = java.util.Base64.getEncoder().encodeToString(encryptedBytes);

        // 2. 加密 userid (固定值为 "1")
        String userId = "1";
        byte[] encryptedUserIdBytes = cipher.doFinal(userId.getBytes("UTF-8"));
        encryptedUserId = java.util.Base64.getEncoder().encodeToString(encryptedUserIdBytes);

        // ==========================================
        // 第三步：调用 /auth/applytoken 获取最终 Token
        // ==========================================
        java.net.URL url2 = new java.net.URL(baseUrl + "/auth/applytoken");
        java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection) url2.openConnection();
        conn2.setRequestMethod("POST");
        
        conn2.setRequestProperty("User-Agent", "PostmanRuntime/7.37.3");
        conn2.setRequestProperty("Accept", "*/*");
        conn2.setRequestProperty("appid", "dangan_sap");
        // 放入加密后的参数
        conn2.setRequestProperty("secret", encryptedSecret); 
        conn2.setRequestProperty("userid", encryptedUserId); 
        conn2.setRequestProperty("Cookie", "ecology_JSessionid=aaa4gn_Zpp2Idf2ViNc6z");
        conn2.setDoOutput(true);

        if (conn2.getResponseCode() != 200) {
            throw new StreamTransformationException("第三步失败，HTTP 状态码: " + conn2.getResponseCode());
        }

        java.io.BufferedReader in2 = new java.io.BufferedReader(new java.io.InputStreamReader(conn2.getInputStream(), "UTF-8"));
        StringBuilder response2 = new StringBuilder();
        String inputLine2;
        while ((inputLine2 = in2.readLine()) != null) { 
            response2.append(inputLine2); 
        }
        in2.close();
        String json2 = response2.toString();

        // 提取最终返回的 token
        java.util.regex.Pattern patternToken = java.util.regex.Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher mToken = patternToken.matcher(json2);
        
        if (mToken.find()) {
            finalToken = mToken.group(1);
        } else {
            throw new StreamTransformationException("解析失败：未在 applytoken 报文中找到 token 字段。");
        }

        // ==========================================
        // 第四步：将 token 和 userid 写入 DynamicConfiguration
        // ==========================================
        DynamicConfiguration dynamicConfig = (DynamicConfiguration) container.getTransformationParameters().get(StreamTransformationConstants.DYNAMIC_CONFIGURATION);
        if (dynamicConfig != null) {
            // 写入 token 属性
            DynamicConfigurationKey keyToken = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/REST", "token");
            dynamicConfig.put(keyToken, finalToken); 
            
            // 写入 userid 属性
            DynamicConfigurationKey keyUserId = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/REST", "userid");
            dynamicConfig.put(keyUserId, encryptedUserId); 
        } else {
            // 如果 Message Mapping 中没抓到动态配置，打印警告日志
            container.getTrace().addWarning("注意: DynamicConfiguration 为空，无法写入 Header 参数。请检查 Sender Channel 是否勾选了 ASMA。");
        }
        
    } catch (Exception e) {
        // 将异常抛出，便于在 PO 的 Message Monitor (NWA) 中排错
        throw new StreamTransformationException("获取泛微 Token 发生异常: " + e.getMessage(), e);
    }
    
    // 返回 token（用于图形化映射连线，即使你只把它连给一个 dummy 字段也没关系）
    return finalToken;
}