package com.company.mapping;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

// 核心修改 1：必须去掉 abstract 关键字，否则类无法被实例化
public class JM_FirstJavaMapping extends AbstractTransformation {

    // PO 系统在运行时，会自动调用这个 transform 方法
    @Override
    public void transform(TransformationInput in, TransformationOutput out) throws StreamTransformationException {
        try {
            // 从 TransformationInput 获取输入流，传入到你自己的 execute 方法中处理
            execute(in.getInputPayload().getInputStream(), out.getOutputPayload().getOutputStream());
        } catch (Exception ee) {
            throw new StreamTransformationException("Mapping failed: " + ee.getMessage(), ee);
        }
    }
    
    // 提取出来的核心处理逻辑（这样写的好处是既能被 PO 调用，也能被 main 方法在本地调用测试）
    public void execute(InputStream in, OutputStream out) throws StreamTransformationException {
        try {
            // 核心修改 2：千万不要用单字节读取！改用 byte 数组作为缓冲区
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            // 每次读取 8KB 的数据包，性能比单字节读取快几千倍
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            out.flush();
            
        } catch (Exception ee) {
            throw new StreamTransformationException("Execute failed: " + ee.getMessage(), ee);
        }
    }
    
    // 本地测试方法（传到 SAP PO 服务器上时，这个方法会被系统忽略，但留着方便你本地打断点调试）
    public static void main(String[] args) {
        try {
            // 提示：确保你的 Eclipse 项目根目录下有一个名为 inputFile.txt 的文件，否则本地运行会报找不到文件
            FileInputStream fin = new FileInputStream("inputFile.txt");
            FileOutputStream fout = new FileOutputStream("outputFile1.txt");
            
            JM_FirstJavaMapping instance = new JM_FirstJavaMapping();
            instance.execute(fin, fout);
            
            System.out.println("本地数据透传测试成功！");
            
            // 核心修改 3：本地测试完毕后，务必养成关闭流的好习惯
            fin.close();
            fout.close();
            
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

	@Override
	public Map<String, Object> getSharedInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSharedInstance(Map<String, Object> arg0) {
		// TODO Auto-generated method stub
		
	}
}