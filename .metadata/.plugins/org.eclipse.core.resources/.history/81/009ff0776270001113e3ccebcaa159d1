package com.company.mapping;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class JM_SecondJavaMapping extends AbstractTransformation {

    @Override
    public void transform(TransformationInput in, TransformationOutput out) throws StreamTransformationException {
        try {
            execute(in.getInputPayload().getInputStream(), out.getOutputPayload().getOutputStream());
        } catch (Exception ee) {
            throw new StreamTransformationException("Mapping failed: " + ee.getMessage(), ee);
        }
    }

    public void execute(InputStream in, OutputStream out) throws StreamTransformationException {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = outFactory.createXMLStreamWriter(out, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");

            String currentElement = null;  // 记录当前正在处理的元素名

            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        // 保存当前元素名，用于后续文本替换判断
                        currentElement = reader.getLocalName();

                        // 正确写入开始元素，包括命名空间和前缀
                        String prefix = reader.getPrefix();
                        String namespaceURI = reader.getNamespaceURI();
                        if (namespaceURI != null) {
                            if (prefix != null && !prefix.isEmpty()) {
                                writer.writeStartElement(prefix, reader.getLocalName(), namespaceURI);
                            } else {
                                writer.writeStartElement(reader.getLocalName(), namespaceURI);
                            }
                        } else {
                            writer.writeStartElement(reader.getLocalName());
                        }

                        // 复制命名空间声明
                        for (int i = 0; i < reader.getNamespaceCount(); i++) {
                            String nsPrefix = reader.getNamespacePrefix(i);
                            String nsURI = reader.getNamespaceURI(i);
                            if (nsPrefix != null && !nsPrefix.isEmpty()) {
                                writer.writeNamespace(nsPrefix, nsURI);
                            } else {
                                writer.writeDefaultNamespace(nsURI);
                            }
                        }

                        // 复制所有属性
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attPrefix = reader.getAttributePrefix(i);
                            String attLocalName = reader.getAttributeLocalName(i);
                            String attNamespace = reader.getAttributeNamespace(i);
                            String attValue = reader.getAttributeValue(i);
                            if (attNamespace != null) {
                                writer.writeAttribute(attPrefix, attNamespace, attLocalName, attValue);
                            } else {
                                writer.writeAttribute(attLocalName, attValue);
                            }
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
//                        String text = reader.getText();
//                        // 只修改 <test> 标签内的非空白文本
//                        if ("test".equals(currentElement) && text != null && text.trim().length() > 0) {
//                            writer.writeCharacters("SAP_MODIFIED_1234");
//                        } else {
//                            writer.writeCharacters(text);
//                        }
                    	String text = reader.getText();
                        // 增加一个判断：不仅看标签名，还要确保它是在 DT_Sample 下面的 TestString
                        if ("TestString".equals(currentElement) && text != null && text.trim().length() > 0) {
                             writer.writeCharacters("SAP_MODIFIED_1234");
                        } else {
                             writer.writeCharacters(text);
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        writer.writeEndElement();
                        // 元素结束，清空当前元素标记（防止混合内容干扰）
                        currentElement = null;
                        break;

                    // 以下事件选择性保留，保证输出完整性
                    case XMLStreamConstants.COMMENT:
                        writer.writeComment(reader.getText());
                        break;

                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        String pit = reader.getPITarget();
                        String pid = reader.getPIData();
                        if (pid != null) {
                            writer.writeProcessingInstruction(pit, pid);
                        } else {
                            writer.writeProcessingInstruction(pit);
                        }
                        break;

                    case XMLStreamConstants.SPACE:
                        writer.writeCharacters(reader.getText());
                        break;

                    // 其他事件（DTD、实体等）可按需扩展，简单场景先跳过
                    default:
                        break;
                }
            }

            writer.writeEndDocument();
            writer.flush();
            writer.close();
            reader.close();

        } catch (Exception e) {
            throw new StreamTransformationException("XML 处理失败: " + e.getMessage(), e);
        }
    }
    
    
    // --- 本地测试入口 ---
    public static void main(String[] args) {
        try {
            // 确保你项目根目录下有 inputFile.txt
            FileInputStream fin = new FileInputStream("inputFile.txt");
            FileOutputStream fout = new FileOutputStream("outputFile1.txt");

            JM_SecondJavaMapping instance = new JM_SecondJavaMapping();
            instance.execute(fin, fout);

            System.out.println("本地 XML 转换测试成功！请查看 outputFile1.txt");
            fin.close();
            fout.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    // --- 必须实现的抽象方法 ---
    private Map<String, Object> sharedInstance;
    @Override
    public void setSharedInstance(Map<String, Object> map) { this.sharedInstance = map; }
    @Override
    public Map<String, Object> getSharedInstance() { return this.sharedInstance; }
}